package de.tozwhv.vereinskasse.server.dto.jobs;

public record JobDTO(
        Long id,
        String type, // "SCHEDULED_FEE", "BACKUP"
        String cronExpression,
        boolean enabled
) {}