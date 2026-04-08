package de.tozwhv.vereinskasse.server.dto;

import java.time.LocalDateTime;

public record TransactionDTO(
        Long id,
        String type,
        String description,
        int amount, // Achtung: Preis pro Stück oder Gesamt? Siehe unten.
        int totalPrice, // Wir brauchen das für dein Frontend!
        LocalDateTime date
) {}