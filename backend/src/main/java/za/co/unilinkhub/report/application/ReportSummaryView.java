package za.co.unilinkhub.report.application;

import za.co.unilinkhub.report.domain.Report;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Report + resolved reporter/target info. ReportDTO stays a plain projection of the aggregate
 * (used for the response right after filing a report); this is the cross-module composed view
 * used both by the admin review queue and by a student's own "my reports" list - either way,
 * nobody should have to look at a raw UUID to know what was reported.
 */
public record ReportSummaryView(
        UUID id,
        String reason,
        String details,
        String status,
        String adminNote,
        LocalDateTime createdAt,
        LocalDateTime resolvedAt,
        ReporterSummary reporter,
        TargetSummary target,
        long totalReportsOnTarget
) {
    public static ReportSummaryView of(Report report, ReporterSummary reporter, TargetSummary target,
                                        long totalReportsOnTarget) {
        return new ReportSummaryView(
                report.getId(), report.getReason().name(), report.getDetails(), report.getStatus().name(),
                report.getAdminNote(), report.getCreatedAt(), report.getResolvedAt(), reporter, target,
                totalReportsOnTarget
        );
    }
}
