package de.tozwhv.vereinskasse.server.service;

import de.tozwhv.vereinskasse.server.dto.PrepaidVoucherDTO;
import de.tozwhv.vereinskasse.server.dto.voucher.IssueVoucherRequest;
import de.tozwhv.vereinskasse.server.dto.voucher.VoucherStatsDTO;
import de.tozwhv.vereinskasse.server.modell.*;
import de.tozwhv.vereinskasse.server.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VoucherService {

    private final PrepaidVoucherRepository voucherRepository;
    private final VoucherRedemptionRepository redemptionRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final SaleRepository saleRepository;

    @Transactional
    public void issueVoucher(IssueVoucherRequest request) {
        User giver = userService.getCurrentUser();
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new RuntimeException("Produkt nicht gefunden"));

        // 1. Geld beim Spender abbuchen (Direkt über das User-Objekt)
        int totalCost = product.getPrice() * request.quantity();

        // Prüfung, ob Spender genug Geld hat (einfache Version)
        if (giver.getBalance() < totalCost) {
            throw new RuntimeException("Guthaben für diese Runde nicht ausreichend!");
        }

        giver.setBalance(giver.getBalance() - totalCost);
        userRepository.save(giver);

        // 2. Voucher-Kontingent erstellen
        PrepaidVoucher voucher = new PrepaidVoucher();
        voucher.setGiver(giver);
        voucher.setProduct(product);
        voucher.setTotalQuantity(request.quantity());
        voucher.setRemainingQuantity(request.quantity());
        voucher.setReason(request.reason());

        voucherRepository.save(voucher);
    }

    @Transactional
    public void redeemVoucher(User consumer, Product product) {
        // Suche den ältesten verfügbaren Voucher (FIFO)
        PrepaidVoucher voucher = voucherRepository
                .findFirstByProductAndRemainingQuantityGreaterThanOrderByCreatedAtAsc(product, 0)
                .orElseThrow(() -> new RuntimeException("Keine Freigetränke mehr verfügbar!"));

        // 1. Kontingent reduzieren
        voucher.setRemainingQuantity(voucher.getRemainingQuantity() - 1);
        voucherRepository.save(voucher);

        // 2. Dokumentation der Einlösung (Das ist unsere "Statistik")
        VoucherRedemption redemption = new VoucherRedemption();
        redemption.setVoucher(voucher);
        redemption.setConsumer(consumer);
        redemptionRepository.save(redemption);

        // Hinweis: Wir brauchen hier keine transactionService.logFreeConsumption mehr,
        // da der SaleService sowieso einen 0€ Sale für die Historie anlegt.
    }

    public List<PrepaidVoucherDTO> getAvailableVouchers() {
        return voucherRepository.findByRemainingQuantityGreaterThan(0).stream()
                .map(v -> new PrepaidVoucherDTO(
                        v.getProduct().getId(),
                        v.getProduct().getName(),
                        v.getRemainingQuantity(),
                        v.getGiver().getUsername(),
                        v.getReason()
                )).toList();
    }

    public List<VoucherStatsDTO> getVoucherStatistics(LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.atTime(LocalTime.MAX);

        // 1. Alle Daten für den Zeitraum laden
        List<PrepaidVoucher> allIssued = voucherRepository.findByCreatedAtBetween(start, end);
        List<Sale> allRedeemed = saleRepository.findByCreatedAtBetweenAndUseVoucherTrue(start, end);
        List<User> users = userRepository.findAll();

        // 2. Pro Benutzer die Summen berechnen
        return users.stream()
                .map(user -> {
                    // Was hat dieser User ausgegeben (Giver)?
                    long issuedVal = allIssued.stream()
                            .filter(v -> v.getGiver().getId().equals(user.getId()))
                            .mapToLong(v -> (long) v.getTotalQuantity() * v.getProduct().getPrice())
                            .sum();

                    long issuedCount = allIssued.stream()
                            .filter(v -> v.getGiver().getId().equals(user.getId()))
                            .mapToLong(PrepaidVoucher::getTotalQuantity)
                            .sum();

                    // Was hat dieser User eingelöst (Consumer)?
                    long redeemedVal = allRedeemed.stream()
                            .filter(s -> s.getUser().getId().equals(user.getId()))
                            .mapToLong(s -> (long) s.getAmount() * s.getProduct().getPrice())
                            .sum();

                    long redeemedCount = allRedeemed.stream()
                            .filter(s -> s.getUser().getId().equals(user.getId()))
                            .mapToLong(Sale::getAmount)
                            .sum();

                    return new VoucherStatsDTO(
                            user.getId(),
                            user.getUsername(),
                            issuedVal,
                            redeemedVal,
                            issuedCount,
                            redeemedCount
                    );
                })
                // Nur User anzeigen, die in diesem Zeitraum aktiv waren
                .filter(dto -> dto.issuedCount() > 0 || dto.redeemedCount() > 0)
                // Sortierung: Wer am meisten spendiert hat zuerst
                .sorted(Comparator.comparingLong(VoucherStatsDTO::totalIssuedValue).reversed())
                .toList();
    }
}