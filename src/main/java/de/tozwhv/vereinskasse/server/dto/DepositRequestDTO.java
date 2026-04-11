package de.tozwhv.vereinskasse.server.dto;

public record DepositRequestDTO(
        Long userId,
        Integer amountInCents
) {
}