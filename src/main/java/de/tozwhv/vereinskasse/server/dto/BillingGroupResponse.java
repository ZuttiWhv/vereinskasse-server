package de.tozwhv.vereinskasse.server.dto;

public record BillingGroupResponse(
        Long id,
        String name,
        String description,
        boolean allowNegativeBalance,
        long creditLimit,
        boolean isDefault
) {}