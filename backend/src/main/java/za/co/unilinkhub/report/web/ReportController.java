package za.co.unilinkhub.report.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import za.co.unilinkhub.report.application.ReportDTO;
import za.co.unilinkhub.report.application.ReportService;
import za.co.unilinkhub.report.application.ReportStatusCounts;
import za.co.unilinkhub.report.application.ReportSummaryView;
import za.co.unilinkhub.report.domain.ReportReason;
import za.co.unilinkhub.report.domain.ReportStatus;
import za.co.unilinkhub.report.domain.ReportTargetType;
import za.co.unilinkhub.security.CurrentUser;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    public record FileReportRequest(
            @NotNull ReportTargetType targetType,
            @NotNull UUID targetId,
            @NotNull ReportReason reason,
            String details
    ) {
    }

    public record ReviewRequest(String note) {
    }

    @PostMapping("/reports")
    @ResponseStatus(HttpStatus.CREATED)
    public ReportDTO file(@CurrentUser UUID userId, @Valid @RequestBody FileReportRequest request) {
        return reportService.file(userId, request.targetType(), request.targetId(), request.reason(), request.details());
    }

    @GetMapping("/reports/mine")
    public List<ReportSummaryView> mine(@CurrentUser UUID userId) {
        return reportService.myReports(userId);
    }

    @GetMapping("/admin/reports")
    @PreAuthorize("hasRole('ADMIN')")
    public List<ReportSummaryView> queue(@RequestParam(required = false) ReportStatus status) {
        return reportService.queue(status);
    }

    @GetMapping("/admin/reports/counts")
    @PreAuthorize("hasRole('ADMIN')")
    public ReportStatusCounts counts() {
        return reportService.counts();
    }

    @PostMapping("/admin/reports/{id}/begin-review")
    @PreAuthorize("hasRole('ADMIN')")
    public ReportSummaryView beginReview(@CurrentUser UUID adminId, @PathVariable UUID id) {
        return reportService.beginReview(id, adminId);
    }

    @PostMapping("/admin/reports/{id}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    public ReportSummaryView resolve(@CurrentUser UUID adminId, @PathVariable UUID id, @RequestBody ReviewRequest request) {
        return reportService.resolve(id, adminId, request.note());
    }

    @PostMapping("/admin/reports/{id}/dismiss")
    @PreAuthorize("hasRole('ADMIN')")
    public ReportSummaryView dismiss(@CurrentUser UUID adminId, @PathVariable UUID id, @RequestBody ReviewRequest request) {
        return reportService.dismiss(id, adminId, request.note());
    }
}
