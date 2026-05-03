package de.tozwhv.vereinskasse.server.service;


import de.tozwhv.vereinskasse.server.modell.User;
import de.tozwhv.vereinskasse.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.openpdf.text.*;
import org.openpdf.text.pdf.Barcode128;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PDFExportService {

    private final UserRepository userRepository;

    public byte[] generateUserBarcodePdf() {
        List<User> users = userRepository.findAll();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // Dokument im A4 Format erstellen
        Document document = new Document(PageSize.A4, 36, 36, 54, 36);
        PdfWriter writer = PdfWriter.getInstance(document, out);

        document.open();

        // 1. Titel hinzufügen
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        Paragraph title = new Paragraph("Mitglieder-Barcodes für Scanner-Login", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(30);
        document.add(title);

        // 2. Tabelle erstellen (3 Spalten: Name, Text, Visueller Barcode)
        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3, 2, 4}); // Breitenverhältnis der Spalten

        // Header setzen
        addTableHeader(table);

        // 3. Benutzerdaten und Barcodes generieren
        for (User user : users) {
            String code = user.getBarcode();
            if (code != null && !code.trim().isEmpty()) {

                // Spalte 1: Name
                table.addCell(new Phrase(user.getUsername()));
                // Spalte 2: Barcode als Klartext
                table.addCell(new Phrase(code));
                // Spalte 3: Der grafische Barcode
                table.addCell(createBarcodeCell(writer, code));
            }
        }

        document.add(table);
        document.close();

        return out.toByteArray();
    }

    private void addTableHeader(PdfPTable table) {
        Font headFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD);
        String[] headers = {"Mitglied", "ID (Text)", "Barcode (Scanbar)"};

        for (String headerText : headers) {
            PdfPCell header = new PdfPCell(new Phrase(headerText, headFont));
            header.setHorizontalAlignment(Element.ALIGN_CENTER);
            header.setPadding(8);
            header.setBackgroundColor(java.awt.Color.LIGHT_GRAY);
            table.addCell(header);
        }
    }

    private PdfPCell createBarcodeCell(PdfWriter writer, String code) {
        // Code 128 ist der Standard für alphanumerische Barcodes
        Barcode128 barcode128 = new Barcode128();
        barcode128.setCode(code);
        barcode128.setFont(null); // Wir zeigen den Text nicht unter den Strichen (ist schon in Spalte 2)

        // Barcode als Image für das PDF erzeugen
        Image barcodeImage = barcode128.createImageWithBarcode(writer.getDirectContent(), null, null);

        PdfPCell cell = new PdfPCell(barcodeImage, true); // true = Bild an Zellengröße anpassen
        cell.setPadding(5);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setFixedHeight(40f); // Einheitliche Höhe für die Zeilen

        return cell;
    }
}