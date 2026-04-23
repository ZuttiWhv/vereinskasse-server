package de.tozwhv.vereinskasse.server.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

@RestController

public class Debuger {
@GetMapping("/api/test-auth")
public ResponseEntity<Map<String, String>> checkAuth(HttpServletRequest request) {
    Map<String, String> info = new HashMap<>();

    // X.509 Zertifikate aus dem Request auslesen
    X509Certificate[] certs = (X509Certificate[])
            request.getAttribute("jakarta.servlet.request.X509Certificate");

    if (certs != null && certs.length > 0) {
        info.put("auth_method", "mTLS");
        info.put("client_dn", certs[0].getSubjectX500Principal().getName());
        info.put("serial_number", certs[0].getSerialNumber().toString());
    } else {
        info.put("auth_method", "None/JWT only");
        info.put("client_dn", "Kein Zertifikat gefunden");
    }

    return ResponseEntity.ok(info);
}}