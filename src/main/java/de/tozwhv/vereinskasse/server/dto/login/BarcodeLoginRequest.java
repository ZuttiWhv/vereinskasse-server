package de.tozwhv.vereinskasse.server.dto.login;

import jakarta.validation.constraints.NotBlank;

public record BarcodeLoginRequest(
        @NotBlank(message = "Barcode darf nicht leer sein")
        String barcode
) {}