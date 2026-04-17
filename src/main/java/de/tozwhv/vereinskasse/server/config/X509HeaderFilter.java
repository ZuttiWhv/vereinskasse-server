package de.tozwhv.vereinskasse.server.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.ByteArrayInputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

@Component
@RequiredArgsConstructor

public class X509HeaderFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, java.io.IOException {

        String headerCert = request.getHeader("X-SSL-CERT");
        if (headerCert != null && !headerCert.isEmpty()) {
            try {
                // Nginx schickt das Zertifikat URL-encoded
                String decodedCert = URLDecoder.decode(headerCert, StandardCharsets.UTF_8);

                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                X509Certificate cert = (X509Certificate) cf.generateCertificate(
                        new ByteArrayInputStream(decodedCert.getBytes(StandardCharsets.UTF_8)));

                // Wir schmuggeln das Zertifikat in das Attribut, das Spring Security erwartet
                request.setAttribute("jakarta.servlet.request.X509Certificate", new X509Certificate[]{cert});
            } catch (Exception e) {
                logger.error("Fehler beim Dekodieren des Client-Zertifikats aus dem Header", e);
            }
        }
        filterChain.doFilter(request, response);
    }
}