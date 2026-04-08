package de.tozwhv.vereinskasse.server.dto;

import java.time.LocalDateTime;

public record SaleDTO(
        long id,
        int price,
        int amount,
        LocalDateTime createdAt,
        long productId,
        String productName,
        long userId,
        String username
) {
}