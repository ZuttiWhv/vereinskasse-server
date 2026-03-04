package de.tozwhv.vereinskasse.server.repository;


import de.tozwhv.vereinskasse.server.modell.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
        Optional<User> findByUsername(String username);
    }

