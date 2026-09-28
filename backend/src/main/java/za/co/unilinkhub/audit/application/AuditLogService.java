package za.co.unilinkhub.audit.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import za.co.unilinkhub.audit.domain.AuditLogEntry;
import za.co.unilinkhub.audit.repository.AuditLogRepository;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Callers resolve {@code adminName} themselves (rather than this service depending on
     * UserService) since UserService itself needs to record entries - a two-way dependency
     * between the two beans would be circular.
     */
    public void record(String adminName, String category, String description) {
        auditLogRepository.save(AuditLogEntry.record(category, description, adminName));
    }

    public List<AuditLogEntryDTO> list(String category, int limit) {
        List<AuditLogEntry> entries = auditLogRepository.findAll();
        if (category != null && !category.isBlank() && !"ALL".equalsIgnoreCase(category)) {
            entries = entries.stream().filter(e -> e.getCategory().equalsIgnoreCase(category)).toList();
        }
        return entries.stream()
                .sorted(Comparator.comparing(AuditLogEntry::getCreatedAt).reversed())
                .limit(limit)
                .map(AuditLogEntryDTO::from)
                .toList();
    }
}
