package de.tozwhv.vereinskasse.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BillingGroupRequest(
        @NotBlank(message = "Der Name der Gruppe darf nicht leer sein")
        @Size(max = 50)
        String name,
        String description,
        boolean allowNegativeBalance,
        long creditLimit,
        boolean isDefault
) {}