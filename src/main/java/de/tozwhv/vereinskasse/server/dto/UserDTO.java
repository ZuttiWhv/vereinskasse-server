package de.tozwhv.vereinskasse.server.dto;

import java.util.Set;

public record UserDTO(
        Long id,
        String username,
        boolean pinEnabled,
        long balance,
        Set<String> roles,
        Set<String> authorities,
        Long billingGroupId,
        String billingGroupName,
        Long orgUnitId,
        String orgUnitName,
        Boolean passwordlessLoginEnabled
) {
}