package de.tozwhv.vereinskasse.server.controller;

import de.tozwhv.vereinskasse.server.dto.role.RoleDTO;
import de.tozwhv.vereinskasse.server.modell.Permission;
import de.tozwhv.vereinskasse.server.modell.Role;
import de.tozwhv.vereinskasse.server.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleRepository roleRepository;

    @GetMapping
    @PreAuthorize("hasAuthority('READ_ROLE')")
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('READ_ROLE')")
    public ResponseEntity<Role> getRoleById(@PathVariable Long id) {
        return roleRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('WRITE_ROLE')")
    @Transactional
    public Role createRole(@RequestBody RoleDTO dto) {
        Role role = new Role();
        mapDtoToEntity(dto, role);
        return roleRepository.save(role);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('WRITE_ROLE')")
    @Transactional
    public ResponseEntity<Role> updateRole(@PathVariable Long id, @RequestBody RoleDTO dto) {
        return roleRepository.findById(id)
                .map(role -> {
                    mapDtoToEntity(dto, role);
                    return ResponseEntity.ok(roleRepository.save(role));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Hilfsmethode zum Mapping vom Record auf die Entity
     */
    private void mapDtoToEntity(RoleDTO dto, Role role) {
        role.setName(dto.name());
        role.setForcePasswordLogin(dto.forcePasswordLogin());
        role.setFeeExempt(dto.feeExempt());
        // Berechtigungen mappen
        if (dto.permissions() != null) {
            Set<Permission> permissions = dto.permissions().stream()
                    .map(p -> {
                        Permission perm = new Permission();
                        perm.setId(p.id());
                        return perm;
                    })
                    .collect(Collectors.toSet());
            role.setPermissions(permissions);
        }
    }
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DELETE_ROLE')")
    public ResponseEntity<Void> deleteRole(@PathVariable Long id) {
        return roleRepository.findById(id)
                .map(role -> {
                    roleRepository.delete(role);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
