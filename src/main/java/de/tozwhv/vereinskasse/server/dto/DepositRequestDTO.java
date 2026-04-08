package de.tozwhv.vereinskasse.server.dto;

public record DepositRequestDTO(
        Long userId,
        Integer amountInCents // Wir bleiben bei Cents für die Genauigkeit
) {}