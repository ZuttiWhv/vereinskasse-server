package de.tozwhv.vereinskasse.server.service;

import de.tozwhv.vereinskasse.server.dto.AppSettingsDTO;
import de.tozwhv.vereinskasse.server.modell.AppSettings;
import de.tozwhv.vereinskasse.server.repository.SettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppSettingsService {

    private static final Long SETTINGS_ID = 1L;
    private final SettingsRepository settingsRepository;

    public AppSettingsService(SettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
    }

    /**
     * Holt die aktuellen Einstellungen oder gibt Standardwerte zurück,
     * falls noch nichts in der Datenbank steht.
     */
    public AppSettingsDTO getSettings() {
        AppSettings s = settingsRepository.findById(SETTINGS_ID)
                .orElse(new AppSettings()); // Nutzt Standardwerte aus dem Konstruktor der Entity

        return mapToDTO(s);
    }

    public AppSettings getSettingsInternal() {
        return settingsRepository.findById(SETTINGS_ID)
                .orElseGet(AppSettings::new);
    }

    /**
     * Aktualisiert die bestehenden Einstellungen.
     */
    @Transactional
    public AppSettingsDTO updateSettings(AppSettingsDTO dto) {
        // Bestehende laden oder neue Entity mit ID 1 erstellen
        AppSettings s = settingsRepository.findById(SETTINGS_ID)
                .orElse(new AppSettings());

        // Werte vom DTO in die Entity übertragen
        s.setPrimaryColor(dto.primaryColor());
        s.setSecondaryColor(dto.secondaryColor());
        s.setNavTextColor(dto.navTextColor());
        s.setVereinName(dto.vereinName());
        s.setLogoPath(dto.logoPath());
        s.setPinLogin(dto.pinLogin());
        s.setQuickLogin(dto.quickLogin());

        // Speichern
        AppSettings updated = settingsRepository.save(s);

        return mapToDTO(updated);
    }

    /**
     * Hilfsmethode zum Umwandeln von Entity -> DTO
     */
    private AppSettingsDTO mapToDTO(AppSettings s) {
        return new AppSettingsDTO(
                s.getPrimaryColor(),
                s.getSecondaryColor(),
                s.getNavTextColor(),
                s.getLogoPath(),
                s.getVereinName(),
                s.isQuickLogin(),
                s.isPinLogin()
        );
    }
}

