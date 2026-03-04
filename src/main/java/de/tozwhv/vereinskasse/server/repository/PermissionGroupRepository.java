package de.tozwhv.vereinskasse.server.repository;

import de.tozwhv.vereinskasse.server.modell.PermissionGroup;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionGroupRepository extends JpaRepository<PermissionGroup, Long> {}