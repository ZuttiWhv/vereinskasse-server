package de.tozwhv.vereinskasse.server.service;

import de.tozwhv.vereinskasse.server.config.JwtProvider;
import de.tozwhv.vereinskasse.server.dto.AuthResponse;
import de.tozwhv.vereinskasse.server.dto.login.PinLoginRequest;
import de.tozwhv.vereinskasse.server.modell.User;
import de.tozwhv.vereinskasse.server.repository.UserRepository;
import io.github.bucket4j.Bucket;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class PinAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final AppSettingsService appSettings;

    // Speicher für die Buckets: Username -> Bucket
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    /**
     * Erstellt eine Bandbreite:
     * Maximal 3 Versuche, danach regeneriert sich 1 Versuch alle 5 Minuten.
     */

    private Bucket createNewBucket() {
            return Bucket.builder()
                    .addLimit(limit -> limit
                            .capacity(3)
                            .refillIntervally(1, Duration.ofMinutes(5)))
                    .build();
        }


    public boolean isPinLoginPossible(String username) {
        return userRepository.findByUsernameAndIsLockedFalseAndActiveTrue(username)
                .map(u -> u.isPinEnabled() && appSettings.getSettingsInternal().isPinLogin())
                .orElse(false);
    }

    // Im PinAuthService.java
    public void resetAttempts(String username) {
        cache.remove(username);
    }

    public AuthResponse verifyPinAndGenerateToken(PinLoginRequest request) {
        String username = request.username();

        // 1. Bucket holen oder neu erstellen
        Bucket bucket = cache.computeIfAbsent(username, _ -> createNewBucket());

        // 2. Prüfen, ob noch Versuche übrig sind
        if (!bucket.tryConsume(1)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Zu viele Fehlversuche für diesen Benutzer. Bitte warten Sie 5 Minuten.");
        }

        User user = userRepository.findByUsernameAndIsLockedFalseAndActiveTrue(username)
                .orElseThrow(() -> new BadCredentialsException("Zugriff verweigert"));

        // 3. Status-Checks
        if (!appSettings.getSettingsInternal().isPinLogin() || !user.isPinEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "PIN-Login deaktiviert");
        }

        // 4. PIN Verifikation
        if (passwordEncoder.matches(request.pin(), user.getPin())) {
            // Bei Erfolg: Bucket leeren/refillen (optional, damit der User danach wieder volle 3 Versuche hat)
            cache.remove(username);

            String access = jwtProvider.generateAccessToken(username);
            String refresh = jwtProvider.generateRefreshToken(username);
            return new AuthResponse(access, refresh);
        } else {
            throw new BadCredentialsException("Falsche PIN. Verbleibende Versuche: " + bucket.getAvailableTokens());
        }
    }
}