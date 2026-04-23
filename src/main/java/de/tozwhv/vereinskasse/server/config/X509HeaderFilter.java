package de.tozwhv.vereinskasse.server.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.ByteArrayInputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class X509HeaderFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, java.io.IOException {

        String headerCert = request.getHeader("X-SSL-CERT");

        if (headerCert != null && !headerCert.isEmpty()) {
            try {
                String decodedCert = URLDecoder.decode(headerCert, StandardCharsets.UTF_8);
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                X509Certificate cert = (X509Certificate) cf.generateCertificate(
                        new ByteArrayInputStream(decodedCert.getBytes(StandardCharsets.UTF_8)));

                request.setAttribute("jakarta.servlet.request.X509Certificate", new X509Certificate[]{cert});

                Authentication existingAuth = SecurityContextHolder.getContext().getAuthentication();

                // Fallstrick 4: Doppeltes Hinzufügen der Rolle verhindern
                SimpleGrantedAuthority deviceAuthority = new SimpleGrantedAuthority("ROLE_TRUSTED_DEVICE");

                if (existingAuth != null && existingAuth.isAuthenticated()) {
                    // USER + DEVICE
                    if (!existingAuth.getAuthorities().contains(deviceAuthority)) {
                        List<GrantedAuthority> updatedAuthorities = new ArrayList<>(existingAuth.getAuthorities());
                        updatedAuthorities.add(deviceAuthority);

                        UsernamePasswordAuthenticationToken combinedAuth = new UsernamePasswordAuthenticationToken(
                                existingAuth.getPrincipal(),
                                existingAuth.getCredentials(),
                                updatedAuthorities
                        );
                        combinedAuth.setDetails(existingAuth.getDetails());
                        SecurityContextHolder.getContext().setAuthentication(combinedAuth);
                        log.info("Zertifikat erkannt: ROLE_TRUSTED_DEVICE zu User '{}' hinzugefügt", existingAuth.getName());
                    }
                } else {
                    // NUR DEVICE (kein JWT vorhanden)
                    String terminalName = cert.getSubjectX500Principal().getName();
                    UsernamePasswordAuthenticationToken deviceOnlyAuth = new UsernamePasswordAuthenticationToken(
                            "DEVICE:" + terminalName, // Eindeutiger Principal-Name
                            null,
                            List.of(deviceAuthority)
                    );
                    SecurityContextHolder.getContext().setAuthentication(deviceOnlyAuth);
                    log.info("Anonymer Request von vertrauenswürdigem Gerät: {}", terminalName);
                }

            } catch (Exception e) {
                log.error("Kritischer Fehler bei Zertifikatsverarbeitung: {}", e.getMessage());
                // Wichtig: Im Fehlerfall keine Auth setzen!
            }
        }

        filterChain.doFilter(request, response);
    }
}