package de.tozwhv.vereinskasse.server.dto.voucher;

public record VoucherStatsDTO(
        Long userId,
        String userName,
        long totalIssuedValue,   // Summe in Cent
        long totalRedeemedValue, // Summe in Cent
        long issuedCount,        // Anzahl spendierter Artikel
        long redeemedCount       // Anzahl eingelöster Artikel
) {}