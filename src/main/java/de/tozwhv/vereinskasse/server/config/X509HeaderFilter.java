package de.tozwhv.vereinskasse.server.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.ByteArrayInputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;

@Component
@RequiredArgsConstructor
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

                // 1. Attribut setzen (für Legacy-Kompatibilität oder andere Filter)
                request.setAttribute("jakarta.servlet.request.X509Certificate", new X509Certificate[]{cert});

                // 2. Rollen-Zuweisung: Wir erstellen eine virtuelle Authentifizierung
                // Wir nehmen den Common Name (CN) des Zertifikats als "Username"
                String terminalName = cert.getSubjectX500Principal().getName();

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        terminalName,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_TRUSTED_DEVICE"))
                );

                // 3. In den SecurityContext legen
                SecurityContextHolder.getContext().setAuthentication(auth);

                logger.info("mTLS Gerät erkannt und autorisiert: " + terminalName);

            } catch (Exception e) {
                logger.error("Fehler beim Dekodieren des Client-Zertifikats", e);
            }
        }

        filterChain.doFilter(request, response);
    }
}