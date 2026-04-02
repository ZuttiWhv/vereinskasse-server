package de.tozwhv.vereinskasse.server.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import de.tozwhv.vereinskasse.server.dto.SaleDTO;
import de.tozwhv.vereinskasse.server.modell.Sale;
import de.tozwhv.vereinskasse.server.modell.User;
import de.tozwhv.vereinskasse.server.repository.ProductRepository;
import de.tozwhv.vereinskasse.server.repository.SaleRepository;
import de.tozwhv.vereinskasse.server.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.server.ResponseStatusException;


import java.util.List;

@RestController
@RequestMapping("/api/sales")
public class SaleController {

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Autowired
    public SaleController(SaleRepository saleRepository, ProductRepository productRepository, UserRepository userRepository) {
        this.saleRepository = saleRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    // Holt alle Verkäufe (Admin) oder nur die eigenen (User)
    @GetMapping
    @PreAuthorize("hasAuthority('READ_ALL_SALES') or hasAuthority('READ_OWN_SALES')")
    public List<Sale> getAllSales(Authentication authentication, @AuthenticationPrincipal User currentUser) {
        if (hasAuthority(authentication, "READ_ALL_SALES")) {
            return saleRepository.findAll();
        }
        // Nutzt die direkte Objekt-Referenz im Repository
        return saleRepository.findByUser(currentUser);
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
            @RequestBody SaleDTO dto,
            Authentication authentication,
            @AuthenticationPrincipal User currentUser) {

        // 1. Validierung (statt ResponseEntity.badRequest() zu senden, werfen wir eine Exception)
        if (dto.getProductId() == null || dto.getAmount() == null || dto.getAmount() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Produkt und gültige Menge erforderlich.");
        }

        // 2. Produkt laden
        var product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produkt nicht gefunden"));

        // 3. Sale Objekt bauen
        Sale sale = new Sale();
        sale.setProduct(product);
        sale.setAmount(dto.getAmount());
        sale.setPrice(product.getPrice() * dto.getAmount());

        // 4. Benutzer zuordnen (Logik bleibt gleich)
        if (hasAuthority(authentication, "WRITE_ALL_SALES") && dto.getUserId() != null) {
            User targetUser = userRepository.findById(dto.getUserId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ziel-Benutzer nicht gefunden"));
            sale.setUser(targetUser);
        } else {
            sale.setUser(currentUser);
        }

        // 5. Speichern und mit festem Typ zurückgeben
        Sale savedSale = saleRepository.save(sale);
        return ResponseEntity.ok(savedSale);
    }


    // Hilfsmethode zur Rollenprüfung
    private boolean hasAuthority(Authentication auth, String authority) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(authority));
    }
}
