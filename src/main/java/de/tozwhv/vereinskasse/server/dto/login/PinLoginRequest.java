package de.tozwhv.vereinskasse.server.dto.login;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * DTO für den Login-Versuch via PIN an einem vertrauenswürdigen Terminal.
 */
public record PinLoginRequest(
        @NotBlank(message = "Benutzername darf nicht leer sein")
        String username,

        @NotBlank(message = "PIN darf nicht leer sein")
        @Pattern(regexp = "\\d{4,6}", message = "PIN muss aus 4 bis 6 Ziffern bestehen")
        String pin
) {}