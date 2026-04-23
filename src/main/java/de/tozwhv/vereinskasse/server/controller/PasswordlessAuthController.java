package de.tozwhv.vereinskasse.server.controller;

import de.tozwhv.vereinskasse.server.dto.AuthResponse;
import de.tozwhv.vereinskasse.server.service.PasswordlessAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth/passwordless")
@RequiredArgsConstructor
public class PasswordlessAuthController {

    private final PasswordlessAuthService passwordlessAuthService;

    /**
     * Prüft, ob für einen Benutzernamen Passwortfreies Login verfügbar ist.
     * Nützlich für das Frontend, um dynamisch zwischen Passwort-
     * PIN-Eingabe oder passwortlosem anmelden zu wechseln.
     */

    @GetMapping("/available/{username}")
    @PreAuthorize("hasRole('TRUSTED_DEVICE')")
    public ResponseEntity<Boolean> isPinAvailable(@PathVariable String username) {
        boolean available = passwordlessAuthService.isPasswordlessPossible(username);
        return ResponseEntity.ok(available);
    }

    /**
     * Endpunkt für den Login ohne Kennwort.
     * Nur für zertifizierte Terminals (TRUSTED_DEVICE) zugänglich.
     */

    @PostMapping("/passwordless")
    @PreAuthorize("hasRole('TRUSTED_DEVICE')")
    public AuthResponse passwordlessLogin(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        return passwordlessAuthService.loginWithoutPassword(username);
    }
}