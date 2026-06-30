package de.tozwhv.vereinskasse.server.repository;

import de.tozwhv.vereinskasse.server.modell.Fee;
import de.tozwhv.vereinskasse.server.modell.User;
import de.tozwhv.vereinskasse.server.modell.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FeeRepository extends JpaRepository<Fee, Long> {

    /**
     * Findet alle Gebühren eines Nutzers in einem bestimmten Zeitraum.
     * Wird direkt für den TransactionService benötigt.
     */
    List<Fee> findByUserAndCreatedAtBetweenOrderByCreatedAtDesc(User user, LocalDateTime start, LocalDateTime end);

    /**
     * Optional: Findet Gebühren nach Typ (nützlich für Auswertungen/Statistiken).
     */
    List<Fee> findByFeeType(TransactionType feeType);

    /**
     * Optional: Findet Gebühren für einen User nach Typ (nützlich für spezifische Abfragen,
     * z.B. alle Aufnahmegebühren eines bestimmten Neumitglieds).
     */
    List<Fee> findByUserAndFeeType(User user, TransactionType feeType);
}