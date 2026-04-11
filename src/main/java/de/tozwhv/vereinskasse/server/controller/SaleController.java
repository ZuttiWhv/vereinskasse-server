package de.tozwhv.vereinskasse.server.controller;

import de.tozwhv.vereinskasse.server.dto.SaleDTO;
import de.tozwhv.vereinskasse.server.dto.SaleRequestDTO;
import de.tozwhv.vereinskasse.server.modell.Sale;
import de.tozwhv.vereinskasse.server.modell.User;
import de.tozwhv.vereinskasse.server.repository.ProductRepository;
import de.tozwhv.vereinskasse.server.repository.SaleRepository;
import de.tozwhv.vereinskasse.server.repository.UserRepository;
import de.tozwhv.vereinskasse.server.service.SaleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/sales")
public class SaleController {

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final SaleService saleService;

    @Autowired
    public SaleController(SaleRepository saleRepository, ProductRepository productRepository, UserRepository userRepository, SaleService saleService) {
        this.saleRepository = saleRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.saleService = saleService;
    }


    @GetMapping
    @PreAuthorize("hasAuthority('READ_ALL_SALES') or hasAuthority('READ_OWN_SALES')")
    public List<SaleDTO> getAllSales(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            Authentication authentication,
            @AuthenticationPrincipal User currentUser) {

        boolean isAdmin = hasAuthority(authentication, "READ_ALL_SALES");
        return saleService.getSalesFiltered(currentUser, start, end, isAdmin);
    }


    // Prüft nach dem Laden, ob der User das Recht hat, diesen speziellen Verkauf zu sehen
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('READ_ALL_SALES') or hasAuthority('READ_OWN_SALES')")
    @PostAuthorize("hasAuthority('READ_ALL_SALES') or returnObject.body.user.id == authentication.principal.id")
    public ResponseEntity<Sale> getSaleById(@PathVariable Long id) {
        return saleRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('WRITE_ALL_SALES') or hasAuthority('WRITE_OWN_SALES')")
    public ResponseEntity<Sale> createSale(
            @RequestBody SaleRequestDTO dto,
            Authentication authentication) { // currentUser entfernt, wir laden ihn selbst

        // 1. Validierung
        if (dto.amount() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ungültige Daten.");
        }

        // 2. Den aktuell angemeldeten User sicher aus der DB laden
        // authentication.getName() liefert den Usernamen aus dem JWT/Session
        User loggedInUser = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Benutzer nicht gefunden."));

        // 3. Produkt laden
        var product = productRepository.findById(dto.productId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produkt nicht gefunden."));

        // 4. Sale Objekt bauen
        Sale sale = new Sale();
        sale.setProduct(product);
        sale.setAmount(dto.amount());
        sale.setPrice(product.getPrice() * dto.amount());

// 5. Benutzer-Zuweisung mit Logik-Trennung
        if (hasAuthority(authentication, "WRITE_ALL_SALES") && dto.userId() != null) {
            // Admin-Modus: Buche für jemand anderen, falls eine ID mitgeliefert wurde
            User targetUser = userRepository.findById(dto.userId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ziel-Benutzer nicht gefunden."));
            sale.setUser(targetUser);
        } else {
            // Standard-Modus ODER Admin bucht für sich selbst (keine userId im DTO)
            sale.setUser(loggedInUser);
        }

        // 6. Guthaben-Logik (wie zuvor besprochen)
        if (sale.getUser().getBalance() < sale.getPrice()) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "Guthaben nicht ausreichend!");
        }

        sale.getUser().setBalance(sale.getUser().getBalance() - sale.getPrice());
        userRepository.save(sale.getUser());

        return ResponseEntity.ok(saleRepository.save(sale));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DELETE_SALES')")
    public ResponseEntity<Void> deleteSale(@PathVariable Long id) {
        // 1. Sale suchen (oder 404 werfen)
        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Verkauf nicht gefunden"));

        // 2. Rückerstattung: Geld dem User wieder gutschreiben
        User user = sale.getUser();
        if (user != null) {
            user.setBalance(user.getBalance() + sale.getPrice());
            userRepository.save(user);
        }

        // 3. Aus der Datenbank löschen
        saleRepository.delete(sale);

        // 4. Rückgabe: 204 No Content (Standard für erfolgreiches Löschen)
        return ResponseEntity.noContent().build();
    }


    // Hilfsmethode zur Rollenprüfung
    private boolean hasAuthority(Authentication auth, String authority) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(authority));
    }
}
