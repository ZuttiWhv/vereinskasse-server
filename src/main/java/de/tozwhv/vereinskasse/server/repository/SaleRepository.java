package de.tozwhv.vereinskasse.server.repository;

import de.tozwhv.vereinskasse.server.modell.Sale;
import de.tozwhv.vereinskasse.server.modell.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {
    List<Sale> findByUser(User user);

    // Holt alle Sales eines Users, sortiert nach Datum (Neu zu Alt)
    List<Sale> findByUserOrderByCreatedAtDesc(User user);
    List<Sale> findAllByOrderByCreatedAtDesc();


}