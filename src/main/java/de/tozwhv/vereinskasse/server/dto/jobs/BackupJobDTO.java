package de.tozwhv.vereinskasse.server.dto.jobs;

public record BackupJobDTO(
        Long id,
        String cronExpression,
        boolean enabled,
        String targetPath,
        Integer retentionDays
) {}