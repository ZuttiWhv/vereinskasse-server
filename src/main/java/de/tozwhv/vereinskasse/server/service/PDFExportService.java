package de.tozwhv.vereinskasse.server.service;


import de.tozwhv.vereinskasse.server.modell.Role;
import de.tozwhv.vereinskasse.server.modell.User;
import de.tozwhv.vereinskasse.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.openpdf.text.*;
import org.openpdf.text.pdf.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PDFExportService {

    private final UserRepository userRepository;

    public byte[] generateUserBarcodePdf() {
        List<User> users = userRepository.findAll();
        users.removeIf(user -> user.getRoles().stream()
                .anyMatch(Role::isForcePasswordLogin));

        // Verwende try-with-resources für den Stream
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 54, 36);
            PdfWriter writer = PdfWriter.getInstance(document, out);

            document.open();

            // Titel
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph title = new Paragraph("Mitglieder-Barcodes für Scanner-Login", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(30);
            document.add(title);

            // Tabelle
            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3, 2, 4});

            addTableHeader(table);

            for (User user : users) {
                String code = user.getBarcode();
                if (code != null && !code.trim().isEmpty()) {
                    table.addCell(new Phrase(user.getUsername()));
                    table.addCell(new Phrase(code));
                    table.addCell(createBarcodeCell(writer, code));
                }
            }

            document.add(table);

            // WICHTIG: Erst das Dokument schließen, dann den Stream auslesen!
            document.close();
            writer.close();

            return out.toByteArray();
        } catch (Exception e) {
            // Logge den Fehler angemessen
            throw new RuntimeException("Fehler beim Erstellen des PDFs", e);
        }
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
        Barcode128 barcode128 = new Barcode128();
        barcode128.setCode(code);
        barcode128.setFont(null); // Entfernt den Text unter dem Barcode

        PdfContentByte cb = writer.getDirectContent();
        // Das Bild wird hier generiert
        Image barcodeImage = barcode128.createImageWithBarcode(cb, null, null);

        // Das 'true' im Konstruktor sorgt dafür, dass das Bild in die Zelle eingepasst wird
        PdfPCell cell = new PdfPCell(barcodeImage, true);
        cell.setPadding(5);
        cell.setFixedHeight(40f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }
}