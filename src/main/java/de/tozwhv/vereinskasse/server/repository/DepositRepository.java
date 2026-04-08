package de.tozwhv.vereinskasse.server.repository;

import de.tozwhv.vereinskasse.server.modell.Deposit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepositRepository extends JpaRepository<Deposit,Long> {
}
