package de.tozwhv.vereinskasse.server.service;

import de.tozwhv.vereinskasse.server.dto.sales.SaleDTO;
import de.tozwhv.vereinskasse.server.dto.sales.SaleRequestDTO;
import de.tozwhv.vereinskasse.server.modell.Product;
import de.tozwhv.vereinskasse.server.modell.Sale;
import de.tozwhv.vereinskasse.server.modell.User;
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

    // Die beiden Services für die Fachlogik
    private final VoucherService voucherService;
    private final AccountingService accountingService;

    /**
     * Verarbeitet einen Verkauf.
     * Entscheidet basierend auf dem DTO, ob ein Gutschein genutzt oder Guthaben belastet wird.
     */
    @Transactional
    public SaleDTO processSale(SaleRequestDTO dto, User loggedInUser, boolean isAdmin) {
        // 1. Stammdaten laden
        User targetUser = determineTargetUser(dto, loggedInUser, isAdmin);
        Product product = productRepository.findById(dto.productId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produkt nicht gefunden."));

        Sale savedSale;

        // 2. Logik-Trennung: Gutschein vs. Guthaben
        if (dto.useVoucher()) {
            // A: Gutschein einlösen
            // Zuerst die Logik im VoucherService (Bestand prüfen/reduzieren)
            voucherService.redeemVoucher(targetUser, product);

            // Dann die Buchung im AccountingService (erstellt den 0€ Sale-Eintrag)
            savedSale = accountingService.bookVoucherRedemption(targetUser, product, dto.amount());
        } else {
            // B: Regulärer Kauf
            // Der AccountingService prüft das Guthaben und zieht den Betrag ab
            savedSale = accountingService.bookRegularSale(targetUser, product, dto.amount());
        }

        return convertToDTO(savedSale);
    }

    /**
     * Holt gefilterte Verkäufe für die Anzeige in der Historie.
     */
    @Transactional(readOnly = true)
    public List<SaleDTO> getSalesFiltered(User currentUser, LocalDateTime start, LocalDateTime end, boolean isAdmin) {
        // Standard-Zeitraum setzen, falls nicht angegeben
        LocalDateTime finalStart = (start == null) ? LocalDateTime.now().minusYears(100) : start;
        LocalDateTime finalEnd = (end == null) ? LocalDateTime.now() : end;

        List<Sale> sales = isAdmin
                ? saleRepository.findAllByCreatedAtBetweenOrderByCreatedAtDesc(finalStart, finalEnd)
                : saleRepository.findByUserAndCreatedAtBetweenOrderByCreatedAtDesc(currentUser, finalStart, finalEnd);

        return sales.stream()
                .map(this::convertToDTO)
                .toList();
    }

    /**
     * Hilfsmethode: Bestimmt, für wen die Buchung durchgeführt wird.
     */
    private User determineTargetUser(SaleRequestDTO dto, User loggedInUser, boolean isAdmin) {
        if (isAdmin && dto.userId() != null) {
            return userRepository.findById(dto.userId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ziel-Benutzer nicht gefunden."));
        }
        return loggedInUser;
    }

    /**
     * Mapping von Entity zu DTO.
     */
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
                sale.isUseVoucher(),
                sale.isVoucher()
        );
    }
}