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
    private final AppSettingsService appSettingsService;

    /**
     * Liefert den kompletten Baum aller Abteilungen und deren User.
     * Berücksichtigt die Einstellung, ob User ohne OU flach oder gruppiert angezeigt werden.
     */
    @Transactional(readOnly = true)
    public List<OrgTreeResponseDTO> getFullOrgTree() {
        // 1. Echte Root-OUs laden
        List<OrganisationalUnit> rootUnits = orgUnitRepository.findByParentIsNull();
        List<OrgTreeResponseDTO> tree = new ArrayList<>(rootUnits.stream()
                .map(this::convertToTreeResponse)
                .toList());

        // 2. Einstellung aus den AppSettings abrufen
        boolean showAsFlatUser = appSettingsService.getSettingsInternal().isShowUserWithoutOuAsUser();

        // 3. User ohne OU holen
        List<User> unassignedUsers = userRepository.findByOrgUnitIdIsNull();

        if (!unassignedUsers.isEmpty()) {
            if (showAsFlatUser) {
                // OPTION A: Jeden User einzeln als "isUser = true" auf die oberste Ebene
                for (User user : unassignedUsers) {
                    tree.add(new OrgTreeResponseDTO(
                            null,
                            user.getUsername(),
                            List.of(),
                            List.of(),
                            true  // Kennzeichnung als User
                    ));
                }
            } else {
                // OPTION B: Alle in eine Pseudo-OU bündeln (isUser = false)
                tree.add(new OrgTreeResponseDTO(
                        -1L,
                        "Nicht zugeordnet",
                        List.of(),
                        unassignedUsers.stream().map(User::getUsername).sorted().toList(),
                        false // Kennzeichnung als Gruppe
                ));
            }
        }

        return tree;
    }

    /**
     * Rekursive Hilfsmethode zur Konvertierung einer Entity in ein Baum-DTO.
     */
    private OrgTreeResponseDTO convertToTreeResponse(OrganisationalUnit unit) {
        // Rekursiver Aufruf für Unterabteilungen
        List<OrgTreeResponseDTO> subUnits = unit.getSubUnits().stream()
                .map(this::convertToTreeResponse)
                .collect(Collectors.toList());

        // Direkt zugeordnete User dieser Einheit
        List<String> usernames = unit.getUsers().stream()
                .map(User::getUsername)
                .sorted()
                .toList();

        return new OrgTreeResponseDTO(
                unit.getId(),
                unit.getName(),
                subUnits,
                usernames,
                false // Eine OU ist niemals ein einzelner User-Knoten
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
     * Gibt alle Organisationseinheiten als flache Liste zurück.
     */
    public List<OrganisationalUnit> getAllUnitsFlat() {
        return orgUnitRepository.findAll();
    }

    /**
     * Löscht eine Organisationseinheit anhand ihrer ID.
     */
    @Transactional
    public void deleteUnit(Long id) {
        orgUnitRepository.deleteById(id);
    }
}