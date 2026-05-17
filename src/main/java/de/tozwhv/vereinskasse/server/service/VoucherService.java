package de.tozwhv.vereinskasse.server.service;

import de.tozwhv.vereinskasse.server.dto.voucher.PrepaidVoucherDTO;
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
    private static final String ARCHIVED_MARKER = "_archived_";
    private final AccountingService accountingService;

    /**
     * Erstellt ein neues Gutschein-Kontingent (eine "Runde").
     * Der Betrag wird dem Spender sofort über den AccountingService abgezogen.
     */
    @Transactional
    public void issueVoucher(IssueVoucherRequest request, boolean isAdmin) {
        User giver = userService.getCurrentUser();
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new RuntimeException("Produkt nicht gefunden"));

        // 1. Finanziellen Teil über den AccountingService abwickeln
        // Das prüft das Guthaben, zieht das Geld ab und erstellt einen Sale-Eintrag (isVoucher = true)
        accountingService.bookVoucherPurchase(giver, product, request.quantity());

        // 2. Das Gutschein-Kontingent in der Datenbank hinterlegen
        PrepaidVoucher voucher = new PrepaidVoucher();
        voucher.setGiver(giver);
        voucher.setProduct(product);
        voucher.setTotalQuantity(request.quantity());
        voucher.setRemainingQuantity(request.quantity());
        voucher.setReason(request.reason());

        voucherRepository.save(voucher);
    }

    /**
     * Reduziert den Bestand eines verfügbaren Gutscheins.
     * Wird vom SaleService aufgerufen, wenn jemand "Gutschein nutzen" wählt.
     */
    @Transactional
    public void redeemVoucher(User consumer, Product product) {
        // Suche den ältesten verfügbaren Voucher für dieses Produkt (FIFO-Prinzip)
        PrepaidVoucher voucher = voucherRepository
                .findFirstByProductAndRemainingQuantityGreaterThanOrderByCreatedAtAsc(product, 0)
                .orElseThrow(() -> new RuntimeException("Keine Freigetränke mehr verfügbar!"));

        // 1. Kontingent reduzieren
        voucher.setRemainingQuantity(voucher.getRemainingQuantity() - 1);
        voucherRepository.save(voucher);

        // 2. Einlösung für die Statistik dokumentieren
        VoucherRedemption redemption = new VoucherRedemption();
        redemption.setVoucher(voucher);
        redemption.setConsumer(consumer);
        redemptionRepository.save(redemption);
    }

    /**
     * Listet alle noch nicht aufgebrauchten Gutscheine auf.
     */
    @Transactional(readOnly = true)
    public List<PrepaidVoucherDTO> getAvailableVouchers() {
        return voucherRepository.findByRemainingQuantityGreaterThan(0).stream()
                .map(v -> {
                    // --- NEU: Schönen Anzeigenamen für den Giver ermitteln ---
                    User giver = v.getGiver();
                    String giverName = giver.getUsername();
                    if (!giver.isActive() && giverName.contains(ARCHIVED_MARKER)) {
                        giverName = giverName.split(ARCHIVED_MARKER)[0] + " (Ehemalig)";
                    }
                    // --------------------------------------------------------

                    return new PrepaidVoucherDTO(
                            v.getProduct().getId(),
                            v.getProduct().getAnzeigename() != null ? v.getProduct().getAnzeigename() : v.getProduct().getName(),
                            v.getRemainingQuantity(),
                            giverName, // GEÄNDERT: Den bereinigten Namen übergeben
                            v.getReason()
                    );
                }).toList();
    }

    /**
     * Erstellt eine Übersicht, wer wie viel spendiert und wer wie viel eingelöst hat.
     */
    @Transactional(readOnly = true)
    public List<VoucherStatsDTO> getVoucherStatistics(LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.atTime(LocalTime.MAX);

        // 1. Daten für den Zeitraum laden
        List<PrepaidVoucher> allIssued = voucherRepository.findByCreatedAtBetween(start, end);
        List<Sale> allRedeemed = saleRepository.findByCreatedAtBetweenAndUseVoucherTrue(start, end);
        List<User> users = userRepository.findAll(); // Lädt aktive UND archivierte User, was für die Historie korrekt ist!

        // 2. Statistiken pro Benutzer aggregieren
        return users.stream()
                .map(user -> {
                    // Summe der ausgegebenen Gutscheine (Giver)
                    long issuedVal = allIssued.stream()
                            .filter(v -> v.getGiver().getId().equals(user.getId()))
                            .mapToLong(v -> (long) v.getTotalQuantity() * v.getProduct().getPrice())
                            .sum();

                    long issuedCount = allIssued.stream()
                            .filter(v -> v.getGiver().getId().equals(user.getId()))
                            .mapToLong(PrepaidVoucher::getTotalQuantity)
                            .sum();

                    // Summe der eingelösten Gutscheine (Consumer)
                    long redeemedVal = allRedeemed.stream()
                            .filter(s -> s.getUser().getId().equals(user.getId()))
                            .mapToLong(s -> (long) s.getAmount() * s.getProduct().getPrice())
                            .sum();

                    long redeemedCount = allRedeemed.stream()
                            .filter(s -> s.getUser().getId().equals(user.getId()))
                            .mapToLong(Sale::getAmount)
                            .sum();

                    // --- NEU: Schönen Anzeigenamen für archivierte Konten ermitteln ---
                    String displayName = user.getUsername();
                    if (!user.isActive() && displayName.contains(ARCHIVED_MARKER)) {
                        displayName = displayName.split(ARCHIVED_MARKER)[0] + " (Ehemalig)";
                    }
                    // -----------------------------------------------------------------

                    return new VoucherStatsDTO(
                            user.getId(),
                            displayName, // GEÄNDERT: Hier den gesäuberten Namen übergeben
                            issuedVal,
                            redeemedVal,
                            issuedCount,
                            redeemedCount
                    );
                })
                .filter(dto -> dto.issuedCount() > 0 || dto.redeemedCount() > 0)
                .sorted(Comparator.comparingLong(VoucherStatsDTO::totalIssuedValue).reversed())
                .toList();
    }
}