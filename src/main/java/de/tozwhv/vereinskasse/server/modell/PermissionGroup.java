package de.tozwhv.vereinskasse.server;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;

import java.util.HashSet;
import java.util.Set;

import static jakarta.persistence.FetchType.LAZY;

@Entity
public class PermissionGroup {
    @Id
    @GeneratedValue
    private Long id;

    private String name;

    @ManyToMany(fetch = LAZY)
    private Set<Permission> permissions = new HashSet<>();
}