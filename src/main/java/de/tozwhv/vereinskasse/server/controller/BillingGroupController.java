package de.tozwhv.vereinskasse.server.controller;

import de.tozwhv.vereinskasse.server.dto.BillingGroupRequest;
import de.tozwhv.vereinskasse.server.dto.BillingGroupResponse;
import de.tozwhv.vereinskasse.server.service.BillingGroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/billing-groups")
@RequiredArgsConstructor
public class BillingGroupController {

    private final BillingGroupService billingGroupService;

    /**
     * Alle Gruppen abrufen.
     * Zugriff: Jeder authentifizierte Benutzer (z.B. für Dropdowns im User-Profil).
     */
    @GetMapping
    @PreAuthorize("hasAuthority('READ_BILLING_GROUP')")
    public ResponseEntity<List<BillingGroupResponse>> getAllGroups() {
        return ResponseEntity.ok(billingGroupService.getAllGroups());
    }

    /**
     * Einzelne Gruppe abrufen.
     */

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('READ_BILLING_GROUP')")
    public ResponseEntity<BillingGroupResponse> getGroupById(@PathVariable Long id) {
        return ResponseEntity.ok(billingGroupService.getGroupById(id));
    }

    /**
     * Neue Gruppe erstellen.
     * Zugriff: Nur Administratoren.
     */

    @PostMapping
    @PreAuthorize("hasAuthority('WRITE_BILLING_GROUP')")
    public ResponseEntity<BillingGroupResponse> createGroup(@Valid @RequestBody BillingGroupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(billingGroupService.createGroup(request));
    }

    /**
     * Gruppe aktualisieren.
     * Zugriff: Nur Administratoren.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('WRITE_BILLING_GROUP')")
    public ResponseEntity<BillingGroupResponse> updateGroup(
            @PathVariable Long id,
            @Valid @RequestBody BillingGroupRequest request) {
        return ResponseEntity.ok(billingGroupService.updateGroup(id, request));
    }

    /**
     * Gruppe löschen.
     * Zugriff: Nur Administratoren.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DELETE_BILLING_GROUP')")
    public ResponseEntity<Void> deleteGroup(@PathVariable Long id) {
        billingGroupService.deleteGroup(id);
        return ResponseEntity.noContent().build();
    }
}