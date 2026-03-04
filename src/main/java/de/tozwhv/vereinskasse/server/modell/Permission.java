package de.tozwhv.vereinskasse.server.modell;

import lombok.*;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

    @Entity
    public class Permission {
        @Id
        @GeneratedValue
        private Long id;
        private String name;
    }

