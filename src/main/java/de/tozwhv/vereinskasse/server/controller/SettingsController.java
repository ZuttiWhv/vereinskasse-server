package de.tozwhv.vereinskasse.server.controller;

import de.tozwhv.vereinskasse.server.dto.AppSettingsDTO;
import de.tozwhv.vereinskasse.server.repository.SettingsRepository;
import de.tozwhv.vereinskasse.server.service.AppSettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final AppSettingsService appSettingsService;

    public SettingsController( AppSettingsService appSettingsService) {
        this.appSettingsService = appSettingsService;
    }

    @GetMapping()
    public ResponseEntity<AppSettingsDTO> getSettings() {
        return ResponseEntity.ok(appSettingsService.getSettings());
    }

    @PutMapping
    @PreAuthorize("hasAuthority('WRITE_SETTINGS')")
    public ResponseEntity<AppSettingsDTO> updateSettings(@RequestBody AppSettingsDTO dto) {
        return ResponseEntity.ok(appSettingsService.updateSettings(dto));
    }
}
