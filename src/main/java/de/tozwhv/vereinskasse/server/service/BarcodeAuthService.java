package de.tozwhv.vereinskasse.server.service;

import de.tozwhv.vereinskasse.server.config.JwtProvider;
import de.tozwhv.vereinskasse.server.dto.AuthResponse;
import de.tozwhv.vereinskasse.server.modell.Role;
import de.tozwhv.vereinskasse.server.modell.User;
import de.tozwhv.vereinskasse.server.repository.UserRepository;
import io.github.bucket4j.Bucket;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class BarcodeAuthService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    // Rate-Limiting Speicher: Schützt vor "Code-Guessing" am Terminal
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    /**
     * Erstellt ein Limit: 10 Scan-Versuche pro Minute pro Terminal.
     * Etwas großzügiger als beim Passwort, da Fehlscans vorkommen können.
     */
    private Bucket createNewBucket() {
        return Bucket.builder()
                .addLimit(limit -> limit
                        .capacity(10)
                        .refillIntervally(10, Duration.ofMinutes(1)))
                .build();
    }

    /**
     * Authentifiziert einen Benutzer anhand seines Barcodes/RFID-UIDs.
     */
    public AuthResponse verifyBarcodeAndGenerateToken(String barcode) {


        // 1. Identifikation des Terminals für das Rate-Limiting
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String terminalKey = (auth != null) ? auth.getName() : "unknown-terminal";

        Bucket bucket = cache.computeIfAbsent(terminalKey, _ -> createNewBucket());
        if (!bucket.tryConsume(1)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Zu viele Scan-Versuche. Bitte warte einen Moment.");
        }

        // 2. mTLS Status prüfen (Sicherheits-Check)
        boolean isTrustedDevice = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_TRUSTED_DEVICE"));

        if (!isTrustedDevice) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Barcode-Login nur an autorisierten Terminals möglich.");
        }

        // User anhand des Barcodes in der DB suchen
        User user = userRepository.findByBarcodeAndIsLockedFalse(barcode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Ungültiger Barcode oder Benutzer nicht gefunden."));


        if (user.getRoles().stream().anyMatch(Role::isForcePasswordLogin)){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Barcode-Login für diesen Account nicht möglich.");
        }

        if (!user.isBarcodeLoginEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Barcode-Login für diesen Account nicht aktiviert.");
        }

        // 5. Erfolg: Token generieren
        String username = user.getUsername();
        String access = jwtProvider.generateAccessToken(username);
        String refresh = jwtProvider.generateRefreshToken(username);

        return new AuthResponse(access, refresh);
    }
}