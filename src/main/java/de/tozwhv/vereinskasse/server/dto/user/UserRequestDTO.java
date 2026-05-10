package de.tozwhv.vereinskasse.server.dto.user;

import java.util.List;

public record UserRequestDTO(
        String username,
        String password,
        List<Long> roleIds,
        Long billingGroupId,
        String pin,
        Long balance,
        Long orgUnitId,
        Boolean passwordlessLoginEnabled,
        String Barcode
) {
}