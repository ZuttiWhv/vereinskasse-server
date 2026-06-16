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
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PDFExportService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ImageResizeService imageResizeService;

    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
    private static final Font CARD_TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
    private static final Font COMPACT_TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
    private static final Font CARD_SUB_FONT = FontFactory.getFont(FontFactory.HELVETICA, 9, java.awt.Color.GRAY);

    public byte[] generateUserBarcodePdf() {
        List<User> users = userRepository.findAll();
        users.removeIf(user -> user.getRoles().stream().anyMatch(Role::isForcePasswordLogin));

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
                noDataCell.setColspan(2);
                noDataCell.setBorder(Rectangle.NO_BORDER);
                noDataCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(noDataCell);
            } else {
                padTable(table, addedCellsCount, 2);
            }
        });
    }

    public byte[] generateProductBarcodePdf() {
        List<Product> products = productRepository.findAllByBarcodesNotEmptyAndActiveIsTrueAndDeletedIsFalseOrderByAnzeigenameDesc();
        return buildGridPdf("Produktübersicht", 2, (table, writer) -> {
            int addedCellsCount = 0;
            for (Product product : products) {
                String code = product.getBarcodes().stream().findFirst().orElse("");
                if (!code.trim().isEmpty()) {
                    table.addCell(createProductGridCell(writer, product, code));
                    addedCellsCount++;
                }
            }
            padTable(table, addedCellsCount, 2);
        });
    }

    public byte[] generateProductBarcodeCompactPdf() {
        List<Product> products = productRepository.findAllByBarcodesNotEmptyAndActiveIsTrueAndDeletedIsFalseOrderByAnzeigenameDesc();
        return buildGridPdf("Produktübersicht (Kompakt)", 3, (table, writer) -> {
            int addedCellsCount = 0;
            for (Product product : products) {
                String code = product.getBarcodes().stream().findFirst().orElse("");
                if (!code.trim().isEmpty()) {
                    table.addCell(createProductCompactGridCell(writer, product, code));
                    addedCellsCount++;
                }
            }
            padTable(table, addedCellsCount, 3);
        });
    }

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
            contentConsumer.accept(table, writer);

            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Fehler bei der PDF-Generierung: {}", titleText, e);
            throw new RuntimeException("PDF Fehler", e);
        }
    }

    private PdfPCell createUserGridCell(PdfWriter writer, User user, String code) throws DocumentException {
        PdfPTable innerTable = new PdfPTable(1);
        innerTable.setWidthPercentage(100);

        PdfPCell nameCell = new PdfPCell(new Phrase(user.getUsername(), CARD_TITLE_FONT));
        nameCell.setBorder(Rectangle.NO_BORDER);
        nameCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        nameCell.setPadding(8);
        innerTable.addCell(nameCell);

        PdfPCell barcodeCell = createBarcodeCell(writer, code, false, 40f);
        barcodeCell.setBorder(Rectangle.NO_BORDER);
        innerTable.addCell(barcodeCell);

        return createWrapperCell(innerTable, 12);
    }

    private PdfPCell createProductGridCell(PdfWriter writer, Product product, String code) throws DocumentException {
        PdfPTable innerTable = new PdfPTable(1);
        innerTable.setWidthPercentage(100);

        innerTable.addCell(createProductImageCell(product.getImagePath()));

        PdfPCell nameCell = new PdfPCell(new Phrase(product.getAnzeigename(), CARD_TITLE_FONT));
        nameCell.setBorder(Rectangle.NO_BORDER);
        nameCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        nameCell.setPadding(5);
        innerTable.addCell(nameCell);

        PdfPCell barcodeCell = createBarcodeCell(writer, code, true, 40f);
        barcodeCell.setBorder(Rectangle.NO_BORDER);
        innerTable.addCell(barcodeCell);

        return createWrapperCell(innerTable, 12);
    }

    private PdfPCell createProductCompactGridCell(PdfWriter writer, Product product, String code) throws DocumentException {
        PdfPTable innerTable = new PdfPTable(1);
        innerTable.setWidthPercentage(100);

        PdfPCell nameCell = new PdfPCell(new Phrase(product.getAnzeigename(), COMPACT_TITLE_FONT));
        nameCell.setBorder(Rectangle.NO_BORDER);
        nameCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        nameCell.setPadding(5);
        innerTable.addCell(nameCell);

        PdfPCell barcodeCell = createBarcodeCell(writer, code, true, 35f);
        barcodeCell.setBorder(Rectangle.NO_BORDER);
        innerTable.addCell(barcodeCell);

        return createWrapperCell(innerTable, 6);
    }

    private PdfPCell createProductImageCell(String imagePath) {
        try {
            if (imagePath != null && !imagePath.trim().isEmpty()) {
                byte[] imageBytes = imageResizeService.resizeImage(imagePath, 200);
                PdfPCell cell = new PdfPCell(Image.getInstance(imageBytes), true);
                cell.setBorder(Rectangle.NO_BORDER);
                cell.setFixedHeight(55f);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                return cell;
            }
        } catch (Exception _) {
            log.warn("Bild konnte nicht geladen werden: {}", imagePath);
        }
        PdfPCell emptyCell = new PdfPCell(new Phrase("[Bild]", CARD_SUB_FONT));
        emptyCell.setBorder(Rectangle.NO_BORDER);
        emptyCell.setFixedHeight(55f);
        emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        emptyCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return emptyCell;
    }

    private PdfPCell createWrapperCell(PdfPTable innerTable, int padding) {
        PdfPCell wrapperCell = new PdfPCell(innerTable);
        wrapperCell.setPadding(padding);
        wrapperCell.setBorderColor(java.awt.Color.LIGHT_GRAY);
        wrapperCell.setBorderWidth(1f);
        return wrapperCell;
    }

    private PdfPCell createBarcodeCell(PdfWriter writer, String code, boolean isEan, float height) {
        PdfContentByte cb = writer.getDirectContent();
        Barcode barcode = isEan ? new BarcodeEAN() : new Barcode128();
        if (isEan) barcode.setCodeType(code.length() == 8 ? Barcode.EAN8 : Barcode.EAN13);
        barcode.setCode(code);
        barcode.setFont(null);
        PdfPCell cell = new PdfPCell(barcode.createImageWithBarcode(cb, null, null), true);
        cell.setFixedHeight(height);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }

    private void padTable(PdfPTable table, int elementCount, int numColumns) {
        int remainder = elementCount % numColumns;
        if (remainder != 0) {
            for (int i = 0; i < (numColumns - remainder); i++) {
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