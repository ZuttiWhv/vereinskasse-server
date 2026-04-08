package de.tozwhv.vereinskasse.server.dto;

import java.util.List;

public record UserRequestDTO(
        String username,
        String password,
        List<Long> roleIds,
        Integer pin,
        Integer balance
) {}