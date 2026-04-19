package de.tozwhv.vereinskasse.server.service;

import de.tozwhv.vereinskasse.server.dto.OrgTreeResponseDTO;
import de.tozwhv.vereinskasse.server.modell.OrganisationalUnit;
import de.tozwhv.vereinskasse.server.modell.User;
import de.tozwhv.vereinskasse.server.repository.OrganisationalUnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrganisationalUnitService {

    private final OrganisationalUnitRepository orgUnitRepository;

    /**
     * Liefert den kompletten Baum aller Abteilungen und deren User.
     * Einstiegspunkte sind alle Einheiten ohne Parent (parent_id IS NULL).
     */
    @Transactional(readOnly = true)
    public List<OrgTreeResponseDTO> getFullOrgTree() {
        List<OrganisationalUnit> rootUnits = orgUnitRepository.findByParentIsNull();
        return rootUnits.stream()
                .map(this::convertToTreeResponse)
                .toList();
    }

    /**
     * Rekursive Hilfsmethode zur Konvertierung einer Entity in ein Baum-DTO.
     */
    private OrgTreeResponseDTO convertToTreeResponse(OrganisationalUnit unit) {
        // Rekursiver Aufruf für alle Unterabteilungen
        List<OrgTreeResponseDTO> subUnits = unit.getSubUnits().stream()
                .map(this::convertToTreeResponse)
                .toList();

        // Extrahiere nur die Benutzernamen der direkt zugeordneten User
        List<String> usernames = unit.getUsers().stream()
                .map(User::getUsername)
                .sorted()
                .toList();

        return new OrgTreeResponseDTO(
                unit.getId(),
                unit.getName(),
                subUnits,
                usernames
        );
    }
    @Transactional
    public OrganisationalUnit createUnit(String name, Long parentId) {
        OrganisationalUnit unit = new OrganisationalUnit();
        unit.setName(name);

        if (parentId != null) {
            OrganisationalUnit parent = orgUnitRepository.findById(parentId)
                    .orElseThrow(() -> new RuntimeException("Parent Unit nicht gefunden"));
            unit.setParent(parent);
        }

        return orgUnitRepository.save(unit);
    }

    public List<OrganisationalUnit> getAllUnitsFlat() {
        return orgUnitRepository.findAll();
    }

    @Transactional
    public void deleteUnit(Long id) {
        // Optional: Prüfung ob noch User in der Einheit sind
        orgUnitRepository.deleteById(id);
    }
}