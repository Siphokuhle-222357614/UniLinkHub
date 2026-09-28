package za.co.unilinkhub.report.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * The trust & safety "detection layer" from the project docs (Section 12.2): lets a student
 * flag a suspicious listing or provider, and gives admins a queue to review and act on it.
 */
@Entity
@Table(name = "reports")
@Getter
@NoArgsConstructor
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "reporter_id", nullable = false)
    private UUID reporterId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private ReportTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReportReason reason;

    @Column(length = 2000)
    private String details;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status = ReportStatus.OPEN;

    @Column(name = "reviewed_by_admin_id")
    private UUID reviewedByAdminId;

    @Column(name = "admin_note", length = 2000)
    private String adminNote;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    private Report(UUID reporterId, ReportTargetType targetType, UUID targetId, ReportReason reason, String details) {
        this.reporterId = reporterId;
        this.targetType = targetType;
        this.targetId = targetId;
        this.reason = reason;
        this.details = details;
    }

    public static Report file(UUID reporterId, ReportTargetType targetType, UUID targetId,
                               ReportReason reason, String details) {
        return new Report(reporterId, targetType, targetId, reason, details);
    }

    public void beginReview(UUID adminId) {
        this.status = ReportStatus.UNDER_REVIEW;
        this.reviewedByAdminId = adminId;
    }

    public void resolve(UUID adminId, String adminNote) {
        this.status = ReportStatus.RESOLVED;
        this.reviewedByAdminId = adminId;
        this.adminNote = adminNote;
        this.resolvedAt = LocalDateTime.now();
    }

    public void dismiss(UUID adminId, String adminNote) {
        this.status = ReportStatus.DISMISSED;
        this.reviewedByAdminId = adminId;
        this.adminNote = adminNote;
        this.resolvedAt = LocalDateTime.now();
    }
}
