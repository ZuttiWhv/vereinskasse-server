package de.tozwhv.vereinskasse.server.dto;

public record UserBalanceDTO(
        Long userId,
        String username,
        Long balanceInCents
) {
}