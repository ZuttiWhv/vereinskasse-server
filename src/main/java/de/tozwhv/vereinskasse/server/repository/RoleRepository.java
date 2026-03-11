package de.tozwhv.vereinskasse.server.repository;


import de.tozwhv.vereinskasse.server.modell.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
}
