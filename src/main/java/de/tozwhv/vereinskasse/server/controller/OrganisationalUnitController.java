package de.tozwhv.vereinskasse.server.controller;

import de.tozwhv.vereinskasse.server.dto.OrgTreeResponseDTO;
import de.tozwhv.vereinskasse.server.service.AppSettingsService;
import de.tozwhv.vereinskasse.server.service.OrganisationalUnitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/org-tree")
@RequiredArgsConstructor
public class OrganisationalUnitController {

    private final OrganisationalUnitService orgUnitService;
    private final AppSettingsService appSettingsService;

    /**
     * Liefert die Organisationsstruktur für den schnellen Login am Terminal.
     * Zugriff ist nur gestattet, wenn sich das Gerät via mTLS (X.509) authentifiziert hat.
     */
    @GetMapping
    @PreAuthorize("hasRole('TRUSTED_DEVICE')")
    public ResponseEntity<List<OrgTreeResponseDTO>> getPublicOrgTree() {
        if (!appSettingsService.getSettingsInternal().isQuickLogin()){
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(orgUnitService.getFullOrgTree());
    }
}