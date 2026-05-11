package de.tozwhv.vereinskasse.server.dto.appsettings;

public record AppSettingsDTO(
        String primaryColor,
        String secondaryColor,
        String navTextColor,
        String logoPath,
        String vereinName,
        boolean quickLogin,
        boolean pinLogin,
        boolean passwordlessLogin,
        boolean allowBarcodeLogin,
        boolean showUserWithoutOuAsUser
) {
}