package za.co.unilinkhub.audit.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.unilinkhub.audit.domain.AuditLogEntry;

import java.util.UUID;

public interface JpaAuditLogRepository extends JpaRepository<AuditLogEntry, UUID>, za.co.unilinkhub.audit.repository.AuditLogRepository {
}
