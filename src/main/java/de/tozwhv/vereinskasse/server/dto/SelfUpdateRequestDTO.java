package de.tozwhv.vereinskasse.server.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SelfUpdateRequestDTO(
        @Size(min = 8, message = "Das Passwort muss mindestens 8 Zeichen lang sein")
        String newPassword,

        @Pattern(regexp = "\\d{4,6}", message = "Die PIN muss aus 4 bis 6 Ziffern bestehen")
        String newPin,

        Boolean pinEnabled
) {}