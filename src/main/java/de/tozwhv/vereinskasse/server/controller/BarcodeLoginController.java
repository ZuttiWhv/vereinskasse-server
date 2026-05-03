package de.tozwhv.vereinskasse.server.controller;

import de.tozwhv.vereinskasse.server.dto.AuthResponse;
import de.tozwhv.vereinskasse.server.dto.login.BarcodeLoginRequest;
import de.tozwhv.vereinskasse.server.service.BarcodeAuthService;
import de.tozwhv.vereinskasse.server.service.AppSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/auth/barcode")
@RequiredArgsConstructor
public class BarcodeAuthController {

    private final BarcodeAuthService barcodeAuthService;
    private final AppSettingsService appSettingsService;

    /**
     * Prüft, ob der Barcode-Login generell in den Systemeinstellungen aktiviert ist.
     * Nützlich für das Frontend, um den Scanner-Listener zu aktivieren/deaktivieren.
     */
    @GetMapping("/enabled")
    @PreAuthorize("hasRole('TRUSTED_DEVICE')")
    public ResponseEntity<Boolean> isBarcodeEnabled() {
        boolean enabled = appSettingsService.getSettingsInternal().isAllowBarcodeLogin();
        return ResponseEntity.ok(enabled);
    }

    /**
     * Endpunkt für den Login via Barcode/RFID.
     * Erwartet den gescannten Code und liefert bei Erfolg ein JWT zurück.
     */
    @PostMapping("/login")
    @PreAuthorize("hasRole('TRUSTED_DEVICE')")
    public ResponseEntity<AuthResponse> loginWithBarcode(@Valid @RequestBody BarcodeLoginRequest request) {
        // 1. Sicherheitsschranke: Ist das Feature überhaupt an?
        if (!appSettingsService.getSettingsInternal().isAllowBarcodeLogin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Barcode-Login ist in den Einstellungen deaktiviert.");
        }

        // 2. Authentifizierung via Service
        // Der Service sucht den User zum Barcode und generiert das Token
        AuthResponse response = barcodeAuthService.verifyBarcodeAndGenerateToken(request.barcode());

        return ResponseEntity.ok(response);
    }
}