package de.tozwhv.vereinskasse.server.service;

import de.tozwhv.vereinskasse.server.dto.OrgTreeResponseDTO;
import de.tozwhv.vereinskasse.server.modell.OrganisationalUnit;
import de.tozwhv.vereinskasse.server.modell.Role;
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
     * Liefert den Baum NUR mit echten Organisationseinheiten (für die Admin-Verwaltung).
     */
    @Transactional(readOnly = true)
    public List<OrgTreeResponseDTO> getPureOrgTree() {
        return buildOrgTree(false);
    }

    /**
     * Liefert den kompletten Baum inklusive der OU-losen User (für das Login-Frontend).
     */
    @Transactional(readOnly = true)
    public List<OrgTreeResponseDTO> getFullOrgTree() {
        return buildOrgTree(true);
    }

    /**
     * Interne Kern-Methode zum Baumaufbau.
     */
    private List<OrgTreeResponseDTO> buildOrgTree(boolean includeVirtualUserNodes) {
        // 1. Echte Root-OUs laden (wichtig: includeVirtualUserNodes steuert, ob gefiltert wird!)
        List<OrganisationalUnit> rootUnits = orgUnitRepository.findByParentIsNull();
        List<OrgTreeResponseDTO> tree = new ArrayList<>(rootUnits.stream()
                .map(unit -> convertToTreeResponse(unit, includeVirtualUserNodes))
                .toList());

        // Wenn wir im Admin-Modus sind, überspringen wir die User-Zuweisung auf Root-Ebene
        if (!includeVirtualUserNodes) {
            return tree;
        }

        // 2. Einstellung aus den AppSettings abrufen
        boolean showAsFlatUser = appSettingsService.getSettingsInternal().isShowUserWithoutOuAsUser();

        // 3. User ohne OU holen und direkt nach QuickLogin-Erlaubnis filtern
        List<User> unassignedUsers = userRepository.findByOrgUnitIdIsNullAndIsLockedFalseAndActiveTrue().stream()
                .filter(this::isQuickLoginAllowed) // Kriterium 1: Nur erlaubte User
                .toList();

        if (!unassignedUsers.isEmpty()) {
            if (showAsFlatUser) {
                for (User user : unassignedUsers) {
                    tree.add(new OrgTreeResponseDTO(
                            null,
                            user.getUsername(),
                            List.of(),
                            List.of(),
                            true
                    ));
                }
            } else {
                tree.add(new OrgTreeResponseDTO(
                        -1L,
                        "Nicht zugeordnet",
                        List.of(),
                        unassignedUsers.stream().map(User::getUsername).sorted().toList(),
                        false
                ));
            }
        }

        return tree;
    }

    /**
     * Rekursive Hilfsmethode zur Konvertierung einer Entity in ein Baum-DTO.
     * @param filterQuickLogin steuert, ob gesperrte Rollen herausgefiltert werden (true fürs Login, false für Admin)
     */
    private OrgTreeResponseDTO convertToTreeResponse(OrganisationalUnit unit, boolean filterQuickLogin) {
        // Rekursiver Aufruf für Unterabteilungen
        List<OrgTreeResponseDTO> subUnits = unit.getSubUnits().stream()
                .map(sub -> convertToTreeResponse(sub, filterQuickLogin))
                .collect(Collectors.toList());

        // Direkt zugeordnete User dieser Einheit ermitteln und optional filtern
        List<String> usernames = unit.getUsers().stream()
                .filter(user -> !filterQuickLogin || isQuickLoginAllowed(user)) // Filter greift nur beim Login-Tree
                .map(User::getUsername)
                .sorted()
                .toList();

        return new OrgTreeResponseDTO(
                unit.getId(),
                unit.getName(),
                subUnits,
                usernames,
                false
        );
    }

    /**
     * Prüft, ob ein Benutzer für den QuickLogin zulässig ist.
     * Sobald MINDESTENS EINE Rolle des Nutzers QuickLogin explizit abgeschaltet hat (z.B. false),
     * fliegt der Nutzer komplett raus.
     */
    private boolean isQuickLoginAllowed(User user) {
        // Angenommen, deine Rolle-Entität besitzt ein boolean-Feld wie 'isQuickLoginEnabled()'
        return user.getRoles().stream()
                .noneMatch(Role::isForcePasswordLogin);
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