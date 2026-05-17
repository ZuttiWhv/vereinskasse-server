package de.tozwhv.vereinskasse.server.controller;

import de.tozwhv.vereinskasse.server.dto.OrgTreeRequestDTO;
import de.tozwhv.vereinskasse.server.dto.OrgTreeResponseDTO;

import de.tozwhv.vereinskasse.server.modell.OrganisationalUnit;
import de.tozwhv.vereinskasse.server.service.AppSettingsService;
import de.tozwhv.vereinskasse.server.service.OrganisationalUnitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/org-units") // Umbenannt für allgemeine Verwaltung
@RequiredArgsConstructor
public class OrganisationalUnitController {

    private final OrganisationalUnitService orgUnitService;
    private final AppSettingsService appSettingsService;

    /**
     * Öffentlicher Baum für das Terminal (Quick-Login).
     * Erfordert mTLS Zertifikat.
     */
    @GetMapping("/tree")
    @PreAuthorize("hasRole('TRUSTED_DEVICE')")
    public ResponseEntity<List<OrgTreeResponseDTO>> getPublicOrgTree() {
        if (!appSettingsService.getSettingsInternal().isQuickLogin()){
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(orgUnitService.getFullOrgTree());
    }

    /**
     *Interner Baum für Admin-Funktionen.
     */
    @GetMapping("/admin-tree")
    @PreAuthorize("hasAuthority('WRITE_OUS')")
    public ResponseEntity<List<OrgTreeResponseDTO>> getInternalOrgTree() {
        return ResponseEntity.ok(orgUnitService.getPureOrgTree());
    }


    /**
     * Admin: Liefert alle OUs (flache Liste) für Verwaltungszwecke.
     */
    @GetMapping
    @PreAuthorize("hasAuthority('READ_OUS')")
    public ResponseEntity<List<OrganisationalUnit>> getAllUnits() {
        return ResponseEntity.ok(orgUnitService.getAllUnitsFlat());
    }

    /**
     * Admin: Erstellt eine neue Organisationseinheit.
     */
    @PostMapping
    @PreAuthorize("hasAuthority('WRITE_OUS')")
    public ResponseEntity<OrganisationalUnit> createUnit(@RequestBody OrgTreeRequestDTO dto) {
        OrganisationalUnit newUnit = orgUnitService.createUnit(dto.name(), dto.parentId());
        return ResponseEntity.status(HttpStatus.CREATED).body(newUnit);
    }

    /**
     * Admin: Löscht eine Organisationseinheit.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DELETE_OUS')")
    public ResponseEntity<Void> deleteUnit(@PathVariable Long id) {
        orgUnitService.deleteUnit(id);
        return ResponseEntity.noContent().build();
    }
}