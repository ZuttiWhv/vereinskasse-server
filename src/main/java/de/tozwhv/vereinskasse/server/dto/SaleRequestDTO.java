package de.tozwhv.vereinskasse.server.dto;

public record SaleRequestDTO(
        int amount,
        Long productId,
        Long userId
) {
}