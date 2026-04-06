package de.tozwhv.vereinskasse.server.service;

import net.coobird.thumbnailator.ThumbnailParameter;
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
                .outputFormat("png") // Einheitliches Format für bessere Kompression
                .outputQuality(0.8)  // 80% Qualität reicht völlig für Thumbnails
                .imageType(ThumbnailParameter.DEFAULT_IMAGE_TYPE) // a.k.a. BufferedImage.TYPE_INT_ARGB
                .toOutputStream(outputStream);

        return outputStream.toByteArray();
    }
}