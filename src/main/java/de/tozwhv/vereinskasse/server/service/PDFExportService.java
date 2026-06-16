package de.tozwhv.vereinskasse.server.service;

import de.tozwhv.vereinskasse.server.modell.Product;
import de.tozwhv.vereinskasse.server.modell.Role;
import de.tozwhv.vereinskasse.server.modell.User;
import de.tozwhv.vereinskasse.server.repository.ProductRepository;
import de.tozwhv.vereinskasse.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openpdf.text.*;
import org.openpdf.text.pdf.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PDFExportService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
    private static final Font CARD_TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
    private static final Font COMPACT_TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
    private static final Font CARD_SUB_FONT = FontFactory.getFont(FontFactory.HELVETICA, 9, java.awt.Color.GRAY);

    public byte[] generateUserBarcodePdf() {
        List<User> users = userRepository.findAll();
        users.removeIf(user -> user.getRoles().stream().anyMatch(Role::isForcePasswordLogin));

        // ANGEPASST: Jetzt 2 Spalten für eine breitere, scansichere Darstellung
        return buildGridPdf("Mitglieder-Barcodes für Scanner-Login", 2, (table, writer) -> {
            int addedCellsCount = 0;

            for (User user : users) {
                String code = user.getBarcode();
                if (code != null && !code.trim().isEmpty()) {
                    table.addCell(createUserGridCell(writer, user, code));
                    addedCellsCount++;
                }
            }

            if (addedCellsCount == 0) {
                PdfPCell noDataCell = new PdfPCell(new Phrase("Keine Mitglieder mit Barcodes gefunden.", CARD_TITLE_FONT));
                noDataCell.setColspan(2); // Auf 2 Spalten angepasst
                noDataCell.setBorder(Rectangle.NO_BORDER);
                noDataCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(noDataCell);
            } else {
                padTable(table, addedCellsCount, 2); // Auf 2 Spalten angepasst
            }
        });
    }

    public byte[] generateProductBarcodePdf() {
        List<Product> products = productRepository.findAllByBarcodesNotEmptyAndActiveIsTrueAndDeletedIsFalseOrderByAnzeigenameDesc();

        // GEBLIEBEN: 2 Spalten für Produkte (Viel Platz für das Bild)
        return buildGridPdf("Produktübersicht", 2, (table, writer) -> {
            int addedCellsCount = 0;

            for (Product product : products) {
                String code = product.getBarcodes().stream().findFirst().orElse("");
                if (!code.trim().isEmpty()) {
                    table.addCell(createProductGridCell(writer, product, code));
                    addedCellsCount++;
                }
            }
            padTable(table, addedCellsCount, 2); // Sicherer Zähler integriert
        });
    }

    /**
     * Kompakter Produkt-Export (Jetzt 3 Spalten statt 4 für leichteres Treffen mit dem Scanner)
     */
    public byte[] generateProductBarcodeCompactPdf() {
        List<Product> products = productRepository.findAllByBarcodesNotEmptyAndActiveIsTrueAndDeletedIsFalseOrderByAnzeigenameDesc();

        // ANGEPASST: Von 4 auf 3 Spalten reduziert
        return buildGridPdf("Produktübersicht (Kompakt)", 3, (table, writer) -> {
            int addedCellsCount = 0;

            for (Product product : products) {
                String code = product.getBarcodes().stream().findFirst().orElse("");
                if (!code.trim().isEmpty()) {
                    table.addCell(createProductCompactGridCell(writer, product, code));
                    addedCellsCount++;
                }
            }
            padTable(table, addedCellsCount, 3); // Auf 3 Spalten angepasst & Sicherer Zähler integriert
        });
    }

    /**
     * Basis-Methode für Grid-Layouts mit optimierten Abständen gegen Fehl-Scans
     */
    private byte[] buildGridPdf(String titleText, int numColumns, PdfContentConsumer contentConsumer) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 54, 36);
            PdfWriter writer = PdfWriter.getInstance(document, out);

            document.open();

            Paragraph title = new Paragraph(titleText, TITLE_FONT);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(30);
            document.add(title);

            PdfPTable table = new PdfPTable(numColumns);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);

            contentConsumer.accept(table, writer);

            document.add(table);
            document.close();
            writer.close();

            return out.toByteArray();
        } catch (Exception e) {
            log.error("Fehler bei der PDF-Generierung von: {}", titleText, e);
            throw new RuntimeException("Fehler beim Erstellen des PDFs", e);
        }
    }

    /**
     * Erstellt eine scan-sichere Kachel für ein Mitglied (Zweispaltig-optimiert)
     */
    private PdfPCell createUserGridCell(PdfWriter writer, User user, String code) throws DocumentException {
        PdfPTable innerTable = new PdfPTable(1);
        innerTable.setWidthPercentage(100);

        // Name zentriert belassen
        Paragraph namePara = new Paragraph(user.getUsername(), CARD_TITLE_FONT);
        namePara.setAlignment(Element.ALIGN_CENTER);
        PdfPCell nameCell = new PdfPCell(namePara);
        nameCell.setBorder(Rectangle.NO_BORDER);
        nameCell.setPaddingTop(8);
        nameCell.setPaddingBottom(6); // Etwas erhöht, da die ID-Zeile wegfällt
        innerTable.addCell(nameCell);

        // GEÄNDERT: Die ID-Textzeile (codePara) wurde hier wunschgemäß entfernt.

        PdfPCell barcodeCell = createBarcodeCell(writer, code, false, 40f);
        barcodeCell.setBorder(Rectangle.NO_BORDER);
        innerTable.addCell(barcodeCell);

        return createWrapperCell(innerTable, 12);
    }

    /**
     * Erstellt eine Kachel für ein Produkt (Standard 2-spaltig mit Bild)
     */
    private PdfPCell createProductGridCell(PdfWriter writer, Product product, String code) throws DocumentException, IOException {
        PdfPTable innerTable = new PdfPTable(1);
        innerTable.setWidthPercentage(100);

        PdfPCell imageCell = createProductImageCell(product.getImagePath());
        innerTable.addCell(imageCell);

        Paragraph namePara = new Paragraph(product.getAnzeigename(), CARD_TITLE_FONT);
        namePara.setAlignment(Element.ALIGN_CENTER);
        PdfPCell nameCell = new PdfPCell(namePara);
        nameCell.setBorder(Rectangle.NO_BORDER);
        nameCell.setPaddingTop(5);
        nameCell.setPaddingBottom(5);
        innerTable.addCell(nameCell);

        PdfPCell barcodeCell = createBarcodeCell(writer, code, true, 40f);
        barcodeCell.setBorder(Rectangle.NO_BORDER);
        innerTable.addCell(barcodeCell);

        return createWrapperCell(innerTable, 12);
    }

    /**
     * Erstellt eine kompakte Kachel (Jetzt 3-spaltig, ohne Bild)
     */
    private PdfPCell createProductCompactGridCell(PdfWriter writer, Product product, String code) throws DocumentException {
        PdfPTable innerTable = new PdfPTable(1);
        innerTable.setWidthPercentage(100);

        Paragraph namePara = new Paragraph(product.getAnzeigename(), COMPACT_TITLE_FONT);
        namePara.setAlignment(Element.ALIGN_CENTER);
        PdfPCell nameCell = new PdfPCell(namePara);
        nameCell.setBorder(Rectangle.NO_BORDER);
        nameCell.setPaddingTop(2);
        nameCell.setPaddingBottom(5);
        innerTable.addCell(nameCell);

        // Durch das 3-Spalten-Layout haben die Barcodes jetzt automatisch mehr
        // horizontale Fläche, was die Erkennungsrate drastisch verbessert.
        PdfPCell barcodeCell = createBarcodeCell(writer, code, true, 35f);
        barcodeCell.setBorder(Rectangle.NO_BORDER);
        innerTable.addCell(barcodeCell);

        return createWrapperCell(innerTable, 6);
    }

    /**
     * Kapselt die inneren Tabellen in eine standardisierte Außenkachel mit variablem Padding
     */
    private PdfPCell createWrapperCell(PdfPTable innerTable, int padding) {
        PdfPCell wrapperCell = new PdfPCell(innerTable);
        wrapperCell.setPadding(padding);
        wrapperCell.setBorderColor(java.awt.Color.LIGHT_GRAY);
        wrapperCell.setBorderWidth(1f);

        wrapperCell.setUseVariableBorders(true);
        wrapperCell.setBorder(Rectangle.BOX);

        return wrapperCell;
    }

    /**
     * Barcode-Methode mit dynamischer Höhen-Zuweisung
     */
    private PdfPCell createBarcodeCell(PdfWriter writer, String code, boolean isEan, float height) {
        PdfContentByte cb = writer.getDirectContent();
        Image barcodeImage;

        if (isEan) {
            BarcodeEAN barcodeEAN = new BarcodeEAN();
            barcodeEAN.setCodeType(code.length() == 8 ? BarcodeEAN.EAN8 : BarcodeEAN.EAN13);
            barcodeEAN.setCode(code);
            barcodeEAN.setFont(null);
            barcodeImage = barcodeEAN.createImageWithBarcode(cb, null, null);
        } else {
            Barcode128 barcode128 = new Barcode128();
            barcode128.setCode(code);
            barcode128.setFont(null);
            barcodeImage = barcode128.createImageWithBarcode(cb, null, null);
        }

        PdfPCell cell = new PdfPCell(barcodeImage, true);
        cell.setPadding(4);
        cell.setFixedHeight(height);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private PdfPCell createProductImageCell(String imagePath) {
        try {
            if (imagePath != null && !imagePath.trim().isEmpty()) {
                Image image = Image.getInstance(imagePath);
                PdfPCell cell = new PdfPCell(image, true);
                cell.setBorder(Rectangle.NO_BORDER);
                cell.setPadding(5);
                cell.setFixedHeight(55f);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                return cell;
            }
        } catch (IOException | DocumentException e) {
            log.warn("Produktbild konnte nicht geladen werden: {}", imagePath);
        }

        PdfPCell emptyCell = new PdfPCell(new Phrase("[Kein Bild]", CARD_SUB_FONT));
        emptyCell.setBorder(Rectangle.NO_BORDER);
        emptyCell.setFixedHeight(55f);
        emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        emptyCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return emptyCell;
    }

    private void padTable(PdfPTable table, int elementCount, int numColumns) {
        int remainder = elementCount % numColumns;
        if (remainder != 0) {
            int missing = numColumns - remainder;
            for (int i = 0; i < missing; i++) {
                PdfPCell emptyCell = new PdfPCell();
                emptyCell.setBorder(Rectangle.NO_BORDER);
                table.addCell(emptyCell);
            }
        }
    }

    @FunctionalInterface
    private interface PdfContentConsumer {
        void accept(PdfPTable table, PdfWriter writer) throws Exception;
    }
}