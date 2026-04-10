package de.tozwhv.vereinskasse.server.dto;

public record AppSettingsDTO(
        String primaryColor,
        String secondaryColor,
        String logoPath,
        String vereinName
) {}