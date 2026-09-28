package za.co.unilinkhub.audit.application;

import za.co.unilinkhub.audit.domain.AuditLogEntry;

import java.time.LocalDateTime;
import java.util.UUID;

public record AuditLogEntryDTO(
        UUID id,
        String category,
        String description,
        String adminName,
        LocalDateTime createdAt
) {
    public static AuditLogEntryDTO from(AuditLogEntry entry) {
        return new AuditLogEntryDTO(entry.getId(), entry.getCategory(), entry.getDescription(),
                entry.getAdminName(), entry.getCreatedAt());
    }
}
