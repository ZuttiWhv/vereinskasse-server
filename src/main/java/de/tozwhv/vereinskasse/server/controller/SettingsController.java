package de.tozwhv.vereinskasse.server.controller;

import de.tozwhv.vereinskasse.server.dto.AppSettingsDTO;
import de.tozwhv.vereinskasse.server.modell.AppSettings;
import de.tozwhv.vereinskasse.server.repository.SaleRepository;
import de.tozwhv.vereinskasse.server.repository.SettingsRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final SettingsRepository settingsRepository;

    public SettingsController(SettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
    }

    @GetMapping()
    public ResponseEntity<AppSettingsDTO> getSettings() {
        AppSettings s = settingsRepository.findById(1L).orElse(new AppSettings());
        return ResponseEntity.ok(new AppSettingsDTO(
                s.getPrimaryColor(), s.getSecondaryColor(), s.getLogoPath(), s.getVereinName()
        ));
    }
}
