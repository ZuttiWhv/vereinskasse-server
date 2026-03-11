package de.tozwhv.vereinskasse.server.controller;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/debug")
public class DebugController {
    private final JwtParser jwtParser; // Die neue Bean

    public DebugController(JwtParser jwtParser) {
        this.jwtParser = jwtParser;
    }

    @GetMapping("/authorities")
    public List<String> getMyAuthorities() {
        // Holt die aktuelle Authentifizierung aus dem SecurityContext
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null) {
            return List.of("Keine Authentifizierung gefunden");
        }

        // Extrahiert die Authorities und wandelt sie in einfache Strings um
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
    }

    @GetMapping("/token-details")
    public Map<String, Object> getTokenDetails(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Map.of("error", "Kein Bearer Token gefunden");
        }

        String token = authHeader.substring(7);

        try {
            // Wir nutzen den injizierten Parser direkt
            Claims claims = jwtParser.parseSignedClaims(token).getPayload();

            return Map.of(
                    "subject", claims.getSubject(),
                    "issuedAt", claims.getIssuedAt(),
                    "expiration", claims.getExpiration(),
                    "type", claims.get("type", String.class),
                    "allClaims", claims // Gibt alle Felder (iss, sub, exp, etc.) zurück
            );
        } catch (Exception e) {
            return Map.of("error", "Token ungültig: " + e.getMessage());
        }
    }
}

