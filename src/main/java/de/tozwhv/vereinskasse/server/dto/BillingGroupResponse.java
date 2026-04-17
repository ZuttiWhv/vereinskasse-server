package de.tozwhv.vereinskasse.server.dto;

import de.tozwhv.vereinskasse.server.modell.BillingGroup;

public record BillingGroupResponse(
        Long id,
        String name,
        String description,
        boolean allowNegativeBalance,
        long creditLimit,
        boolean isDefault
) {
    public static BillingGroupResponse fromEntity(BillingGroup group) {
        return new BillingGroupResponse(
                group.getId(),
                group.getName(),
                group.getDescription(),
                group.isAllowNegativeBalance(),
                group.getCreditLimit(),
                group.isDefault()
        );

    }
}




