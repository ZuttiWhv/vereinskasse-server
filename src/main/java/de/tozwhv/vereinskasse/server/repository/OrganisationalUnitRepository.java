package de.tozwhv.vereinskasse.server.repository;

import de.tozwhv.vereinskasse.server.modell.OrganisationalUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrganisationalUnitRepository extends JpaRepository<OrganisationalUnit, Long> {
    // Findet alle Top-Level Abteilungen
    List<OrganisationalUnit> findByParentIsNull();
}