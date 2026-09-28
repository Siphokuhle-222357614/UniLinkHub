package za.co.unilinkhub.audit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A read-only trail of admin actions (verifications, account changes, report decisions,
 * announcements) so admins can see who did what across the console, not just the current
 * state of each item.
 */
@Entity
@Table(name = "audit_log_entries")
@Getter
@NoArgsConstructor
public class AuditLogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 30)
    private String category;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(name = "admin_name", nullable = false, length = 200)
    private String adminName;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private AuditLogEntry(String category, String description, String adminName) {
        this.category = category;
        this.description = description;
        this.adminName = adminName;
    }

    public static AuditLogEntry record(String category, String description, String adminName) {
        return new AuditLogEntry(category, description, adminName);
    }
}
