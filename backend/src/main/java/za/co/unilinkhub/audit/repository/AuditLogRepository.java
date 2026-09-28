package za.co.unilinkhub.audit.repository;

import za.co.unilinkhub.audit.domain.AuditLogEntry;

import java.util.List;

public interface AuditLogRepository {
    AuditLogEntry save(AuditLogEntry entry);
    List<AuditLogEntry> findAll();
}
