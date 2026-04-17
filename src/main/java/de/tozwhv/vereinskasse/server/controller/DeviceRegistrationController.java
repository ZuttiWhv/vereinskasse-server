package de.tozwhv.vereinskasse.server.controller;

import de.tozwhv.vereinskasse.server.dto.DeviceRegistrationDTO;
import de.tozwhv.vereinskasse.server.service.DeviceCertificateService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/devices")
public class DeviceRegistrationController {

    private final DeviceCertificateService certService;

    public DeviceRegistrationController(DeviceCertificateService certService) {
        this.certService = certService;
    }

    @PostMapping("/register")
    @PreAuthorize("hasAuthority('WRITE_DEVICE')")
    public ResponseEntity<byte[]> registerDevice(@RequestBody DeviceRegistrationDTO request) throws Exception {
            byte[] p12File = certService.createDeviceCertificate(request.name());

            // Datenbank-Logik hier: request.getName() und Zertifikat-Seriennummer speichern

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + request.name() + ".p12\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(p12File);
    }
    @GetMapping("/ca-public")
    @PreAuthorize("hasAuthority('WRITE_DEVICE')")
    //@PreAuthorize("hasRole('WRITE_DEVICE')")
    public ResponseEntity<byte[]> getPublicCA() throws Exception {
        byte[] caBytes = certService.getPublicCACertificate();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Vereinskasse_Root_CA.crt\"")
                // Offizieller MIME-Type für X.509 Zertifikate
                .contentType(MediaType.parseMediaType("application/x-x509-ca-cert"))
                .body(caBytes);
    }
}