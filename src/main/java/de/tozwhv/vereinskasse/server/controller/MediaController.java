package de.tozwhv.vereinskasse.server.controller;

import de.tozwhv.vereinskasse.server.service.ImageResizeService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/media") // WICHTIG: Anderer Pfad als /media/
public class MediaController {

    private final ImageResizeService resizeService;

    public MediaController(ImageResizeService resizeService) {
        this.resizeService = resizeService;
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
}