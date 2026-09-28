package za.co.unilinkhub.audit.web;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import za.co.unilinkhub.audit.application.AuditLogEntryDTO;
import za.co.unilinkhub.audit.application.AuditLogService;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping("/api/admin/audit-log")
    @PreAuthorize("hasRole('ADMIN')")
    public List<AuditLogEntryDTO> list(@RequestParam(defaultValue = "ALL") String category,
                                        @RequestParam(defaultValue = "50") int limit) {
        return auditLogService.list(category, limit);
    }
}
