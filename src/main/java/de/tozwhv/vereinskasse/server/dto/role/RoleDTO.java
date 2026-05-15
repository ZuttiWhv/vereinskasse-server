package de.tozwhv.vereinskasse.server.dto.role;

import de.tozwhv.vereinskasse.server.dto.permission.PermissionIdDTO;

import java.util.List;

public record RoleDTO(
        String name,
        boolean forcePasswordLogin,
        List<PermissionIdDTO> permissions
) {}