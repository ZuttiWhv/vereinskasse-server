package de.tozwhv.vereinskasse.server.service;

import de.tozwhv.vereinskasse.server.dto.OrgTreeResponseDTO;
import de.tozwhv.vereinskasse.server.modell.OrganisationalUnit;
import de.tozwhv.vereinskasse.server.modell.User;
import de.tozwhv.vereinskasse.server.repository.OrganisationalUnitRepository;
import de.tozwhv.vereinskasse.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrganisationalUnitService {

    private final OrganisationalUnitRepository orgUnitRepository;
    private final UserRepository userRepository;

    /**
     * Liefert den kompletten Baum aller Abteilungen und deren User.
     * Ergänzt eine virtuelle Gruppe für User ohne OU-Zuordnung.
     */
    @Transactional(readOnly = true)
    public List<OrgTreeResponseDTO> getFullOrgTree() {
        // 1. Alle echten Root-Einheiten (parent_id IS NULL) abrufen und konvertieren
        List<OrganisationalUnit> rootUnits = orgUnitRepository.findByParentIsNull();
        List<OrgTreeResponseDTO> tree = new ArrayList<>(rootUnits.stream()
                .map(this::convertToTreeResponse)
                .collect(Collectors.toList()));

        // 2. User finden, die KEINER Organisationseinheit zugeordnet sind
        List<String> unassignedUsernames = userRepository.findByOrgUnitIdIsNull()
                .stream()
                .map(User::getUsername)
                .sorted()
                .toList();

        // 3. Virtuellen Knoten hinzufügen, falls es nicht zugeordnete User gibt
        if (!unassignedUsernames.isEmpty()) {
            tree.add(new OrgTreeResponseDTO(
                    -1L,                // Statische ID für "Nicht zugeordnet"
                    "Nicht zugeordnet",  // Anzeigename im Tree
                    List.of(),          // Keine Sub-Einheiten
                    unassignedUsernames // Die Liste der Usernamen
            ));
        }

        return tree;
    }

    /**
     * Rekursive Hilfsmethode zur Konvertierung einer Entity in ein Baum-DTO.
     */
    private OrgTreeResponseDTO convertToTreeResponse(OrganisationalUnit unit) {
        // Rekursiver Aufruf für alle Unterabteilungen (Kinder)
        List<OrgTreeResponseDTO> subUnits = unit.getSubUnits().stream()
                .map(this::convertToTreeResponse)
                .collect(Collectors.toList());

        // Extrahiere die Benutzernamen der direkt dieser Einheit zugeordneten User
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

    /**
     * Erstellt eine neue Organisationseinheit.
     */
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

    /**
     * Gibt alle Organisationseinheiten als flache Liste zurück (z.B. für Dropdowns).
     */
    public List<OrganisationalUnit> getAllUnitsFlat() {
        return orgUnitRepository.findAll();
    }

    /**
     * Löscht eine Organisationseinheit anhand ihrer ID.
     */
    @Transactional
    public void deleteUnit(Long id) {
        // Hinweis: Hier könnte man prüfen, ob noch User zugeordnet sind,
        // bevor man das Löschen zulässt.
        orgUnitRepository.deleteById(id);
    }
}