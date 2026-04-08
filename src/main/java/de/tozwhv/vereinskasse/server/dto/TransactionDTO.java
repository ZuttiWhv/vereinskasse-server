package de.tozwhv.vereinskasse.server.dto;

import java.time.LocalDateTime;

public record TransactionDTO(
        Long id,
        String type, // "SALE" oder "DEPOSIT"
        String description,
        int amount,
        LocalDateTime date
) {}