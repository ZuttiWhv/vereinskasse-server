package de.tozwhv.vereinskasse.server.dto;

import java.util.List;

public record OrgTreeResponseDTO(
        Long id,
        String name,
        List<OrgTreeResponseDTO> subUnits,
        List<String> usernames,
        boolean isUser //
) {
}

