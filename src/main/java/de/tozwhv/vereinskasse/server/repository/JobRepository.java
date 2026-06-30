package de.tozwhv.vereinskasse.server.repository;

import de.tozwhv.vereinskasse.server.modell.systemjob.SystemJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobRepository extends JpaRepository<SystemJob, Long> {
    List<SystemJob> findByEnabledTrue();
    List<SystemJob> getAllBy();
}