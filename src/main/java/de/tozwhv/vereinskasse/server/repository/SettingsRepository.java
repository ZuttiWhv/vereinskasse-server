package de.tozwhv.vereinskasse.server.repository;

import de.tozwhv.vereinskasse.server.modell.AppSettings;
import de.tozwhv.vereinskasse.server.modell.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface SettingsRepository extends JpaRepository<AppSettings, Long> {

}

