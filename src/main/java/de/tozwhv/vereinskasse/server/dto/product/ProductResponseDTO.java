package de.tozwhv.vereinskasse.server.dto.product;

public record ProductResponseDTO(
        Long id,
        String name,
        String anzeigename,
        int price,
        String priceString,
        String imagePath,
        CategorySummaryDTO category,
        boolean active,
        String barcode
) {
    /**
     * Kompaktes DTO für die Kategorie-Info innerhalb des Produkts
     */
    public record CategorySummaryDTO(Long id, String name) {}
}