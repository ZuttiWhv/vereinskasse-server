package de.tozwhv.vereinskasse.server.repository;


import de.tozwhv.vereinskasse.server.modell.BillingGroup;
import de.tozwhv.vereinskasse.server.modell.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByBillingGroup(BillingGroup billingGroup);
    Optional<User> findByBarcode(String barcode);
    List<User> findByOrgUnitIdIsNullAndIsLockedFalseAndActiveTrue();
    List<User> findAllByIsLockedFalseAndActiveTrue();
    Optional<User> findByBarcodeAndIsLockedFalseAndActiveTrue(String barcode);
    Optional<User> findByUsernameAndIsLockedFalseAndActiveTrue(String username);
}

