package de.tozwhv.vereinskasse.server.controller;

import de.tozwhv.vereinskasse.server.dto.AuthResponse;
import de.tozwhv.vereinskasse.server.dto.login.PinLoginRequest;
import de.tozwhv.vereinskasse.server.service.PinAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/pin")
@RequiredArgsConstructor
public class PinAuthController {

    private final PinAuthService pinAuthService;

    /**
     * Prüft, ob für einen Benutzernamen der PIN-Login verfügbar ist.
     * Nützlich für das Frontend, um dynamisch zwischen Passwort-
     * und PIN-Eingabe zu wechseln.
     */
    @GetMapping("/available/{username}")
    @PreAuthorize("hasRole('TRUSTED_DEVICE')")
    public ResponseEntity<Boolean> isPinAvailable(@PathVariable String username) {
        boolean available = pinAuthService.isPinLoginPossible(username);
        return ResponseEntity.ok(available);
    }

    /**
     * Endpunkt für den Login via PIN.
     * Nur für zertifizierte Terminals (TRUSTED_DEVICE) zugänglich.
     */
    @PostMapping("/login")
    @PreAuthorize("hasRole('TRUSTED_DEVICE')")
    public ResponseEntity<AuthResponse> loginWithPin(@Valid @RequestBody PinLoginRequest request) {
        // Der Service wirft bei Fehlern (falsche PIN, zu viele Versuche)
        // entsprechende Exceptions, die global abgefangen werden.
        AuthResponse token = pinAuthService.verifyPinAndGenerateToken(request);

        // Wir geben das JWT-Token in der gewohnten AuthResponse zurück
        return ResponseEntity.ok(token);
    }
}