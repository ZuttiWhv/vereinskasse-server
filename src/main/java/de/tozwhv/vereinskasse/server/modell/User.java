package de.tozwhv.vereinskasse.server;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;

import java.util.HashSet;
import java.util.Set;

import static jakarta.persistence.FetchType.LAZY;

@Entity
public class User {
    @Id
    @GeneratedValue
    private Long id;
    private String username;
    private String password;

    @ManyToMany(fetch = LAZY)
    private Set<Role> roles = new HashSet<>();

    @ManyToMany(fetch = LAZY)
    private Set<PermissionGroup> permissionGroups = new HashSet<>();
}
