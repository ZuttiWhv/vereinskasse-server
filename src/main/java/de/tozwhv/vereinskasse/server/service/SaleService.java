package de.tozwhv.vereinskasse.server.service;

import de.tozwhv.vereinskasse.server.dto.sales.SaleDTO;
import de.tozwhv.vereinskasse.server.dto.sales.SaleRequestDTO;
import de.tozwhv.vereinskasse.server.modell.Sale;
import de.tozwhv.vereinskasse.server.modell.User;
import de.tozwhv.vereinskasse.server.modell.Product;
import de.tozwhv.vereinskasse.server.repository.ProductRepository;
import de.tozwhv.vereinskasse.server.repository.SaleRepository;
import de.tozwhv.vereinskasse.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SaleService {

    private final SaleRepository saleRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final VoucherService voucherService;

    /**
     * Verarbeitet einen Verkauf.
     * Prüft, ob ein Freigetränk (Voucher) genutzt werden soll oder ob vom Guthaben abgebucht wird.
     */
    @Transactional
    public Sale processSale(SaleRequestDTO dto, User loggedInUser, boolean isAdmin) {
        // 1. Ziel-Benutzer bestimmen (Admin darf für andere buchen)
        User targetUser = determineTargetUser(dto, loggedInUser, isAdmin);

        // 2. Produkt laden
        Product product = productRepository.findById(dto.productId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produkt nicht gefunden."));

        Sale sale = new Sale();
        sale.setProduct(product);
        sale.setAmount(dto.amount());
        sale.setUser(targetUser);

        // --- NEU: Flag für die Statistik setzen ---
        sale.setUseVoucher(dto.useVoucher());

        // 3. Logik-Trennung: Voucher vs. Guthaben
        if (dto.useVoucher()) {
            // Freigetränk einlösen (Voucher-Bestand reduzieren)
            // Hinweis: Wir nutzen hier dto.amount(), falls ein User mehrere Voucher gleichzeitig einlöst
            voucherService.redeemVoucher(targetUser, product);

            // Für die Buchhaltung/Historie bleibt der Zahlpreis 0
            sale.setPrice(0);
        } else {
            // Regulärer Kauf: Preis berechnen und Guthaben prüfen
            int totalPrice = product.getPrice() * dto.amount();
            validateBalance(targetUser, totalPrice);

            // Abbuchen und Speichern des neuen Kontostands
            targetUser.setBalance(targetUser.getBalance() - totalPrice);
            userRepository.save(targetUser);

            sale.setPrice(totalPrice);
        }

        // 4. Verkauf in der Historie speichern
        return saleRepository.save(sale);
    }

    /**
     * Holt gefilterte Verkäufe für die Anzeige in der Historie.
     */
    @Transactional(readOnly = true)
    public List<SaleDTO> getSalesFiltered(User currentUser, LocalDateTime start, LocalDateTime end, boolean isAdmin) {
        List<Sale> sales;

        if (start == null) start = LocalDateTime.now().minusYears(100);
        if (end == null) end = LocalDateTime.now();

        if (isAdmin) {
            sales = saleRepository.findAllByCreatedAtBetweenOrderByCreatedAtDesc(start, end);
        } else {
            sales = saleRepository.findByUserAndCreatedAtBetweenOrderByCreatedAtDesc(currentUser, start, end);
        }

        return sales.stream()
                .map(this::convertToDTO)
                .toList();
    }

    private User determineTargetUser(SaleRequestDTO dto, User loggedInUser, boolean isAdmin) {
        if (isAdmin && dto.userId() != null) {
            return userRepository.findById(dto.userId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ziel-Benutzer nicht gefunden."));
        }
        return loggedInUser;
    }

    private void validateBalance(User user, double requiredAmount) {
        // Prüfung unter Berücksichtigung des Kreditlimits der BillingGroup
        long creditLimit = user.getBillingGroup().isAllowNegativeBalance() ? user.getBillingGroup().getCreditLimit() : 0;
        if ((user.getBalance() + creditLimit) < requiredAmount) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "Guthaben nicht ausreichend!");
        }
    }

    private SaleDTO convertToDTO(Sale sale) {
        return new SaleDTO(
                sale.getId(),
                sale.getPrice(),
                sale.getAmount(),
                sale.getCreatedAt(),
                sale.getProduct().getId(),
                sale.getProduct().getAnzeigename() != null ? sale.getProduct().getAnzeigename() : sale.getProduct().getName(),
                sale.getUser().getId(),
                sale.getUser().getUsername(),
                sale.isUseVoucher() // Falls das DTO auch angepasst wurde, hier mitgeben
        );
    }
}