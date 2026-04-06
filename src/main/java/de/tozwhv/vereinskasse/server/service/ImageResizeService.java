package de.tozwhv.vereinskasse.server.service;

import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class ImageResizeService {

    @Value("${app.upload-dir}")
    private String uploadDir;

    public byte[] resizeImage(String filename, int width) throws IOException {
        // 1. Originaldatei finden
        Path originalPath = Paths.get(uploadDir).resolve(filename).toAbsolutePath();

        // 2. Thumbnail generieren (im Speicher)
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        Thumbnails.of(originalPath.toFile())
                .width(width) // Höhe wird automatisch proportional berechnet
                .outputFormat("jpg") // Einheitliches Format für bessere Kompression
                .outputQuality(0.8)  // 80% Qualität reicht völlig für Thumbnails
                .toOutputStream(outputStream);

        return outputStream.toByteArray();
    }
}