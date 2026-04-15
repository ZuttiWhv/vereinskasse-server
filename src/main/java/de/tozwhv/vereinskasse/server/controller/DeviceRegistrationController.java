package de.tozwhv.vereinskasse.server.controller;

import de.tozwhv.vereinskasse.server.dto.DeviceRegistrationDTO;
import de.tozwhv.vereinskasse.server.service.DeviceCertificateService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/devices")
public class DeviceRegistrationController {

    private DeviceCertificateService certService;

    @PostMapping("/register")
    @PreAuthorize("hasRole('DEVICE_WRITE')")
    public ResponseEntity<byte[]> registerDevice(@RequestBody DeviceRegistrationDTO request) {
        try {
            byte[] p12File = certService.createDeviceCertificate(request.name());

            // Datenbank-Logik hier: request.getName() und Zertifikat-Seriennummer speichern

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + request.name() + ".p12\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(p12File);
        } catch (Exception _) {
            return ResponseEntity.internalServerError().build();
        }
    }
}