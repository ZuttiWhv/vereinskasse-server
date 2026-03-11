package de.tozwhv.vereinskasse.server.modell;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

import static jakarta.persistence.FetchType.EAGER;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

@Entity
public class Role {
    @Id
    @GeneratedValue
    private Long id;
    private String name;

    @ManyToMany(fetch = EAGER)
    @JoinTable(
            name = "role_permission",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions = new HashSet<>();

}