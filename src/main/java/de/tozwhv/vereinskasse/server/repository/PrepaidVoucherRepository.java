package de.tozwhv.vereinskasse.server.repository;

import de.tozwhv.vereinskasse.server.modell.PrepaidVoucher;
import de.tozwhv.vereinskasse.server.modell.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PrepaidVoucherRepository extends JpaRepository<PrepaidVoucher, Long> {

    /**
     * Findet das älteste verfügbare Kontingent für ein bestimmtes Produkt.
     * 'RemainingQuantity > 0' stellt sicher, dass noch etwas da ist.
     * 'OrderByCreatedAtAsc' implementiert das FIFO-Prinzip (älteste zuerst).
     */
    Optional<PrepaidVoucher> findFirstByProductAndRemainingQuantityGreaterThanOrderByCreatedAtAsc(
            Product product,
            Integer minQuantity
    );

    /**
     * Findet alle Vouchers, die noch nicht vollständig aufgebraucht sind.
     * Nützlich für die Anzeige im Frontend (z.B. "Diese Freigetränke sind gerade im Pool").
     */
    List<PrepaidVoucher> findByRemainingQuantityGreaterThan(Integer minQuantity);

    /**
     * Findet alle Vouchers eines bestimmten Spenders (für Statistiken/Historie).
     */
    List<PrepaidVoucher> findByGiverIdOrderByCreatedAtDesc(Long giverId);

    List<PrepaidVoucher> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

}