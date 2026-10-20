package de.tozwhv.vereinskasse.server.dto.sales;

public record SaleRequestDTO(
        Long productId,
        Integer amount,
        Long userId,            // Nullable: Pflicht bei normalen Käufen
        String guestName,       // Nullable: Pflicht bei Gast-Käufen
        Long hostId,            // Nullable: Optionales bürgendes Mitglied
        Boolean useVoucher,
        Boolean isVoucher
) {
    public SaleRequestDTO {
        if (useVoucher == null) useVoucher = false;
        if (isVoucher == null) isVoucher = false;

        // Sicherheitsprüfung: Ein Gast darf keine Gutscheine nutzen
        if (guestName != null && useVoucher) {
            throw new IllegalArgumentException("Gäste können keine Gutscheine einlösen.");
        }

        // Validierung: Es muss entweder ein Mitglied ODER ein Gast angegeben sein
        if (userId == null && (guestName == null || guestName.isBlank())) {
            throw new IllegalArgumentException("Der Verkauf muss entweder einem Mitglied oder einem Gast zugewiesen werden.");
        }
    }
}