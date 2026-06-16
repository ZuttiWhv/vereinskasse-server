package de.tozwhv.vereinskasse.server.controller;

import de.tozwhv.vereinskasse.server.service.PDFExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
public class ExportController {

    private final PDFExportService pdfExportService;

    @GetMapping("/user-barcodes-pdf")
    @PreAuthorize("hasAuthority('EXPORT_USER_BARCODES')")
    public ResponseEntity<byte[]> downloadUserBarcodePdf() {
        byte[] pdfContent = pdfExportService.generateUserBarcodePdf();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=mitglieder_barcodes.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfContent);
    }

    @GetMapping("/product-barcodes-pdf")
    @PreAuthorize("hasAuthority('EXPORT_PRODUCT_BARCODES')")
    public ResponseEntity<byte[]> downloadProductBarcodePdf() {
        byte[] pdfContent = pdfExportService.generateProductBarcodePdf();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=produkt_barcodes.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfContent);
    }

    @GetMapping("/product-barcodes-compact-pdf")
    @PreAuthorize("hasAuthority('EXPORT_PRODUCT_BARCODES')")
    public ResponseEntity<byte[]> downloadProductBarcodeCompactPdf() {
        byte[] pdfContent = pdfExportService.generateProductBarcodeCompactPdf();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=produkt_barcodes_kompakt.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfContent);
    }
}