package de.tozwhv.vereinskasse.server.modell;

import lombok.*;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;
import static jakarta.persistence.FetchType.LAZY;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

@Entity
public class PermissionGroup {
    @Id
    @GeneratedValue
    private Long id;

    private String name;

    @ManyToMany(fetch = LAZY)
    @JoinTable(
            name = "permission_group_permissions",
            joinColumns = @JoinColumn(name = "permission_group_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions = new HashSet<>();
}