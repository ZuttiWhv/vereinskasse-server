package de.tozwhv.vereinskasse.server.dto;

public record OrgTreeRequestDTO(
        String name,
        Long parentId) {
}