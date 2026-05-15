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
public class PasswordlessAuthService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    // Speicher für Rate-Limiting (Brute-Force Schutz für Usernames)
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    /**
     * Erstellt ein Limit: 5 Versuche pro Minute für den passwortfreien Login.
     */
    private Bucket createNewBucket() {
        return Bucket.builder()
                .addLimit(limit -> limit
                        .capacity(5)
                        .refillIntervally(5, Duration.ofMinutes(1)))
                .build();
    }

    /**
     * Prüft, ob der User für passwortfreien Login berechtigt ist UND
     * ob das aktuell genutzte Gerät via mTLS autorisiert wurde.
     */
    public boolean isPasswordlessPossible(String username) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Prüfen, ob die mTLS-Rolle im SecurityContext vorhanden ist
        boolean isTrustedDevice = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_TRUSTED_DEVICE"));

        if (!isTrustedDevice) return false;
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return false;

        if (user.getRoles().stream().anyMatch(Role::isForcePasswordLogin)){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Barcode-Login für diesen Account nicht möglich.");
        }

        return user.isPasswordlessLoginEnabled();
    }

    /**
     * Generiert ein JWT Token ohne Passwortabfrage, sofern mTLS vorhanden ist.
     */
    public AuthResponse loginWithoutPassword(String username) {
        // 1. Rate Limiting prüfen
        Bucket bucket = cache.computeIfAbsent(username, _ -> createNewBucket());
        if (!bucket.tryConsume(1)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Zu viele Anfragen. Bitte kurz warten.");
        }

        // 2. mTLS Status prüfen (Sicherheits-Check)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isTrustedDevice = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_TRUSTED_DEVICE"));

        if (!isTrustedDevice) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Passwortfreier Login nur an autorisierten Terminals möglich.");
        }

        // 3. User laden und prüfen, ob Flag in DB gesetzt ist
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Benutzer nicht gefunden"));

        if (user.getRoles().stream().anyMatch(Role::isForcePasswordLogin)){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Passwortfreier-Login für diesen Account nicht möglich.");
        }

        if (!user.isPasswordlessLoginEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Passwortfreier Login für diesen Account nicht gestattet.");
        }

        // 4. Erfolg: Token generieren
        String access = jwtProvider.generateAccessToken(username);
        String refresh = jwtProvider.generateRefreshToken(username);

        // Rate-Limit nach Erfolg zurücksetzen
        cache.remove(username);

        return new AuthResponse(access, refresh);
    }
}