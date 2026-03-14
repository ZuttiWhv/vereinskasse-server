package de.tozwhv.vereinskasse.server.controller;

import de.tozwhv.vereinskasse.server.modell.Sale;
import de.tozwhv.vereinskasse.server.modell.User;
import de.tozwhv.vereinskasse.server.repository.SaleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sales")
public class SaleController {

    private final SaleRepository saleRepository;

    @Autowired
    public SaleController(SaleRepository saleRepository) {
        this.saleRepository = saleRepository;
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

    // Speichert einen neuen Verkauf und erzwingt den aktuellen User als Besitzer
    @PostMapping
    @PreAuthorize("hasAuthority('WRITE_ALL_SALES') or hasAuthority('WRITE_OWN_SALES')")
    public Sale createSale(@RequestBody Sale sale, @AuthenticationPrincipal User currentUser) {
        // Sicherheit: Überschreibt mitschickte user_id im JSON, außer man ist Admin
        // (Wichtig, damit User A keine Verkäufe für User B buchen kann)
        sale.setUser(currentUser);
        return saleRepository.save(sale);
    }

    // Hilfsmethode zur Rollenprüfung
    private boolean hasAuthority(Authentication auth, String authority) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(authority));
    }
}
