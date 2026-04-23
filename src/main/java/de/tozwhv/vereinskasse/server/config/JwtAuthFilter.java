package de.tozwhv.vereinskasse.server.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j // Besseres Logging
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // Fallstrick 1: Header-Format prüfen (Groß-/Kleinschreibung und Länge)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = authHeader.substring(7).trim();

            // Fallstrick 2: Leere Token abfangen
            if (token.isEmpty()) {
                filterChain.doFilter(request, response);
                return;
            }

            if (jwtProvider.isValid(token)) {
                String username = jwtProvider.getUsername(token);

                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails user = userDetailsService.loadUserByUsername(username);

                    if (user != null && user.isEnabled()) { // Nur aktive User zulassen
                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

                        // Fallstrick 3: Web-Details (IP, Session-ID) für Auditing mitgeben
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                        SecurityContextHolder.getContext().setAuthentication(authToken);
                        log.debug("JWT Auth erfolgreich für User: {}", username);
                    }
                }
            }
        } catch (Exception ex) {
            // Wir loggen den Fehler, lassen den Request aber weiterlaufen.
            // Die AccessDenied-Logik von Spring Security greift später,
            // falls der Endpunkt @PreAuthorize benötigt.
            log.warn("JWT Validierung fehlgeschlagen: {}", ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}