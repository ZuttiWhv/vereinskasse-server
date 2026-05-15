package de.tozwhv.vereinskasse.server.controller;

import de.tozwhv.vereinskasse.server.dto.voucher.PrepaidVoucherDTO;
import de.tozwhv.vereinskasse.server.dto.voucher.IssueVoucherRequest;
import de.tozwhv.vereinskasse.server.dto.voucher.VoucherStatsDTO;
import de.tozwhv.vereinskasse.server.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;


    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('READ_ALL_SALES')")
    public List<VoucherStatsDTO> getStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        return voucherService.getVoucherStatistics(from, to);
    }

    /**
     * Erstellt eine neue Spende (Runde).
     * Der eingeloggte User bezahlt sofort den Gesamtbetrag.
     */
    @PostMapping("/issue")
    @PreAuthorize("hasAuthority('WRITE_OWN_SALES')")
    public ResponseEntity<Void> issueVoucher(@RequestBody IssueVoucherRequest request, Authentication authentication) {
        boolean isAdmin = hasAuthority(authentication, "WRITE_ALL_SALES");
        voucherService.issueVoucher(request,isAdmin);
        return ResponseEntity.ok().build();
    }

    /**
     * Gibt eine Liste aller aktuell verfügbaren Freigetränke-Kontingente zurück.
     * Hilfreich für das Frontend, um zu zeigen: "12x Bier verfügbar (Spender: Vorstand)"
     */
    @GetMapping("/available")
    @PreAuthorize("hasAuthority('WRITE_OWN_SALES')")
    public ResponseEntity<List<PrepaidVoucherDTO>> getAvailableVouchers() {
        return ResponseEntity.ok(voucherService.getAvailableVouchers());
    }

    // Hilfsmethode zur Rollenprüfung
    private boolean hasAuthority(Authentication auth, String authority) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(authority));
    }

}