package de.tozwhv.vereinskasse.server.controller;

import de.tozwhv.vereinskasse.server.dto.sales.SaleDTO;
import de.tozwhv.vereinskasse.server.dto.sales.SaleRequestDTO;
import de.tozwhv.vereinskasse.server.modell.Sale;
import de.tozwhv.vereinskasse.server.modell.User;
import de.tozwhv.vereinskasse.server.repository.ProductRepository;
import de.tozwhv.vereinskasse.server.repository.SaleRepository;
import de.tozwhv.vereinskasse.server.repository.UserRepository;
import de.tozwhv.vereinskasse.server.service.SaleService;
import de.tozwhv.vereinskasse.server.service.UserService;
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
    private final UserRepository userRepository;
    private final SaleService saleService;
    private final UserService userService;

    @Autowired
    public SaleController(SaleRepository saleRepository, ProductRepository productRepository, UserRepository userRepository, SaleService saleService, UserService userService) {
        this.saleRepository = saleRepository;
        this.userRepository = userRepository;
        this.saleService = saleService;
        this.userService = userService;
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
    public ResponseEntity<Sale> createSale(@RequestBody SaleRequestDTO dto, Authentication authentication) {

        // User aus DB laden (wie bisher)
        User loggedInUser = userService.getCurrentUser();
        boolean isAdmin = hasAuthority(authentication, "WRITE_ALL_SALES");

        // Die komplette Magie passiert im Service
        Sale sale = saleService.processSale(dto, loggedInUser, isAdmin);

        return ResponseEntity.ok(sale);
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
