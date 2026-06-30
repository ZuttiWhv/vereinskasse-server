package de.tozwhv.vereinskasse.server.dto.jobs;

public record ScheduledFeeJobDTO(
        Long id,
        String cronExpression,
        boolean enabled,
        Integer amountInCents,
        String description
) {}
