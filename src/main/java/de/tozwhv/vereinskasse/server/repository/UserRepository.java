package de.tozwhv.vereinskasse.server.repository;


import de.tozwhv.vereinskasse.server.modell.BillingGroup;
import de.tozwhv.vereinskasse.server.modell.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByBillingGroup(BillingGroup billingGroup);
    Optional<User> findByBarcodeAndIsLockedFalse(String barcode);
}

