package de.tozwhv.vereinskasse.server.dto.product;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProductRequestDTO(
        @NotBlank(message = "Der technische Name ist erforderlich")
        String name,

        String anzeigename,

        @Min(value = 0, message = "Der Preis darf nicht negativ sein")
        int price,

        String imagePath,

        @NotNull(message = "Eine Kategorie-ID muss angegeben werden")
        Long categoryId,

        boolean active,

        String barcode
) {}