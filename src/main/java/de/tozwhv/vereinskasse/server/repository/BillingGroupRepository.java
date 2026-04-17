package de.tozwhv.vereinskasse.server.repository;

import de.tozwhv.vereinskasse.server.modell.BillingGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface BillingGroupRepository extends JpaRepository<BillingGroup, Long> {

}

