package de.tozwhv.vereinskasse.server.repository;

import de.tozwhv.vereinskasse.server.modell.VoucherRedemption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VoucherRedemptionRepository extends JpaRepository<VoucherRedemption, Long> {

    /**
     * Findet alle Verzehr-Vorgänge für einen bestimmten Voucher.
     * Nützlich, um zu sehen: "Wer hat von Michaels Geburtstagsrunde getrunken?"
     */
    List<VoucherRedemption> findByVoucherIdOrderByRedeemedAtDesc(Long voucherId);

    /**
     * Findet alle Verzehr-Vorgänge eines bestimmten Benutzers.
     * Nützlich für die persönliche Historie: "Welche Freigetränke habe ich genutzt?"
     */
    List<VoucherRedemption> findByConsumerIdOrderByRedeemedAtDesc(Long consumerId);
}