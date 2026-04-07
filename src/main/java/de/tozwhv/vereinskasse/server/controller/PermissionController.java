package de.tozwhv.vereinskasse.server.controller;

import de.tozwhv.vereinskasse.server.modell.Permission;
import de.tozwhv.vereinskasse.server.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionRepository permissionRepository;

    @GetMapping
    @PreAuthorize("hasAuthority('READ_PERMISSION')")
    public List<Permission> getAllPermissions() {
        return permissionRepository.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('READ_PERMISSION')")
    public ResponseEntity<Permission> getPermissionById(@PathVariable Long id) {
        return permissionRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

}
