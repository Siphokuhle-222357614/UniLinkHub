package za.co.unilinkhub.report.application;

import za.co.unilinkhub.report.domain.Report;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReportDTO(
        UUID id,
        UUID reporterId,
        String targetType,
        UUID targetId,
        String reason,
        String details,
        String status,
        UUID reviewedByAdminId,
        String adminNote,
        LocalDateTime createdAt,
        LocalDateTime resolvedAt
) {
    public static ReportDTO from(Report report) {
        return new ReportDTO(
                report.getId(), report.getReporterId(), report.getTargetType().name(), report.getTargetId(),
                report.getReason().name(), report.getDetails(), report.getStatus().name(),
                report.getReviewedByAdminId(), report.getAdminNote(), report.getCreatedAt(), report.getResolvedAt()
        );
    }
}
