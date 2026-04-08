package de.tozwhv.vereinskasse.server.controller;

import de.tozwhv.vereinskasse.server.service.FileStorageService;
import de.tozwhv.vereinskasse.server.service.ImageResizeService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/media") // WICHTIG: Anderer Pfad als /media/
public class MediaController {

    private final ImageResizeService resizeService;
    private final FileStorageService storageService; // Hinzugefügt

    public MediaController(ImageResizeService resizeService, FileStorageService storageService) {
        this.resizeService = resizeService;
        this.storageService = storageService;
    }

    @GetMapping("/{filename}")
    public ResponseEntity<byte[]> getImage(
            @PathVariable String filename,
            @RequestParam(defaultValue = "80") int width) { // Standardbreite 80px

        try {
            byte[] imageBytes = resizeService.resizeImage(filename, width);

            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .header("Cache-Control", "public, max-age=31536000") // 1 Jahr Browser-Cache!
                    .body(imageBytes);
        } catch (IOException _) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/upload")
    @PreAuthorize("hasAuthority('UPLOAD_IMAGES')") // Unabhängig von Kategorie/Produkt
    public ResponseEntity<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        String filename = storageService.storeFile(file);
        if (filename != null) {
            return ResponseEntity.ok(Map.of("filename", filename));
        } else {
            return ResponseEntity.internalServerError().build();
        }
    }

}