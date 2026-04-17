package de.tozwhv.vereinskasse.server.service;

import de.tozwhv.vereinskasse.server.dto.BillingGroupRequest;
import de.tozwhv.vereinskasse.server.dto.BillingGroupResponse;
import de.tozwhv.vereinskasse.server.modell.BillingGroup;
import de.tozwhv.vereinskasse.server.repository.BillingGroupRepository;
import de.tozwhv.vereinskasse.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class BillingGroupService {

    private final BillingGroupRepository billingGroupRepository;
    private final UserRepository userRepository;

    /**
     * Gibt alle Abrechnungsgruppen als Response-DTOs zurück.
     */
    @Transactional(readOnly = true)
    public List<BillingGroupResponse> getAllGroups() {
        return billingGroupRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Findet eine Gruppe nach ID.
     */
    @Transactional(readOnly = true)
    public BillingGroupResponse getGroupById(Long id) {
        BillingGroup group = billingGroupRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Abrechnungsgruppe nicht gefunden"));
        return mapToResponse(group);
    }

    /**
     * Erstellt eine neue Abrechnungsgruppe.
     */
    @Transactional
    public BillingGroupResponse createGroup(BillingGroupRequest request) {
        // Falls die neue Gruppe als Default markiert ist, bestehende Default-Flags entfernen
        if (request.isDefault()) {
            resetDefaultFlags();
        }

        BillingGroup group = new BillingGroup();
        updateEntity(group, request);

        return mapToResponse(billingGroupRepository.save(group));
    }

    /**
     * Aktualisiert eine bestehende Gruppe.
     */
    @Transactional
    public BillingGroupResponse updateGroup(Long id, BillingGroupRequest request) {
        BillingGroup group = billingGroupRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Abrechnungsgruppe nicht gefunden"));

        if (request.isDefault() && !group.isDefault()) {
            resetDefaultFlags();
        }

        updateEntity(group, request);
        return mapToResponse(billingGroupRepository.save(group));
    }

    /**
     * Löscht eine Gruppe, sofern keine User mehr zugeordnet sind.
     */
    @Transactional
    public void deleteGroup(Long id) {
        BillingGroup group = billingGroupRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Abrechnungsgruppe nicht gefunden"));

        // Sicherheitscheck: Verhindern, dass die Gruppe gelöscht wird, wenn noch User drin sind
        if (userRepository.existsByBillingGroup(group)) {
            throw new IllegalStateException("Gruppe kann nicht gelöscht werden, da ihr noch Benutzer zugeordnet sind.");
        }

        // Sicherheitscheck: Die letzte Standardgruppe sollte man nicht löschen können
        if (group.isDefault()) {
            throw new IllegalStateException("Die Standard-Abrechnungsgruppe kann nicht gelöscht werden.");
        }

        billingGroupRepository.delete(group);
    }

    /**
     * Hilfsmethode: Prüft das Limit eines Benutzers für eine Transaktion.
     */
    public boolean isTransactionAllowed(Long userId, long debitAmount) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User nicht gefunden"));

        long newBalance = user.getBalance() - debitAmount;
        BillingGroup group = user.getBillingGroup();

        if (group == null || !group.isAllowNegativeBalance()) {
            return newBalance >= 0;
        }

        // Limit ist als positiver Wert gespeichert (z.B. 5000 für 50€ Dispo)
        return newBalance >= (group.getCreditLimit() * -1);
    }

    private void resetDefaultFlags() {
        billingGroupRepository.findAll().forEach(g -> {
            if (g.isDefault()) {
                g.setDefault(false);
                billingGroupRepository.save(g);
            }
        });
    }

    private void updateEntity(BillingGroup group, BillingGroupRequest request) {
        group.setName(request.name());
        group.setDescription(request.description());
        group.setAllowNegativeBalance(request.allowNegativeBalance());
        group.setCreditLimit(request.creditLimit());
        group.setDefault(request.isDefault());
    }

    private BillingGroupResponse mapToResponse(BillingGroup group) {
        return new BillingGroupResponse(
                group.getId(),
                group.getName(),
                group.getDescription(),
                group.isAllowNegativeBalance(),
                group.getCreditLimit(),
                group.isDefault()
        );
    }
}