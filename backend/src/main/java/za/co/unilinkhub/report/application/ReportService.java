package za.co.unilinkhub.report.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import za.co.unilinkhub.audit.application.AuditLogService;
import za.co.unilinkhub.business.domain.Business;
import za.co.unilinkhub.business.repository.BusinessRepository;
import za.co.unilinkhub.common.exception.ResourceNotFoundException;
import za.co.unilinkhub.listing.domain.Listing;
import za.co.unilinkhub.listing.repository.ListingRepository;
import za.co.unilinkhub.report.domain.Report;
import za.co.unilinkhub.report.domain.ReportReason;
import za.co.unilinkhub.report.domain.ReportStatus;
import za.co.unilinkhub.report.domain.ReportTargetType;
import za.co.unilinkhub.report.repository.ReportRepository;
import za.co.unilinkhub.user.domain.User;
import za.co.unilinkhub.user.repository.UserRepository;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final ListingRepository listingRepository;
    private final BusinessRepository businessRepository;
    private final AuditLogService auditLogService;

    public ReportDTO file(UUID reporterId, ReportTargetType targetType, UUID targetId, ReportReason reason, String details) {
        Report report = Report.file(reporterId, targetType, targetId, reason, details);
        return ReportDTO.from(reportRepository.save(report));
    }

    public List<ReportSummaryView> myReports(UUID reporterId) {
        return reportRepository.findByReporterId(reporterId).stream()
                .sorted(Comparator.comparing(Report::getCreatedAt).reversed())
                .map(this::toSummaryView)
                .toList();
    }

    public List<ReportSummaryView> reportsAgainst(UUID userId) {
        List<UUID> ownedListingIds = businessRepository.findByOwnerId(userId).stream()
                .flatMap(b -> listingRepository.findByBusinessId(b.getId()).stream())
                .map(Listing::getId)
                .toList();

        return reportRepository.findAll().stream()
                .filter(r -> (r.getTargetType() == ReportTargetType.USER && r.getTargetId().equals(userId))
                        || (r.getTargetType() == ReportTargetType.LISTING && ownedListingIds.contains(r.getTargetId())))
                .sorted(Comparator.comparing(Report::getCreatedAt).reversed())
                .map(this::toSummaryView)
                .toList();
    }

    public List<ReportSummaryView> queue(ReportStatus status) {
        List<Report> reports = status == null ? reportRepository.findAll() : reportRepository.findByStatus(status);
        return reports.stream()
                .sorted(Comparator.comparing(Report::getCreatedAt).reversed())
                .map(this::toSummaryView)
                .toList();
    }

    public ReportStatusCounts counts() {
        return new ReportStatusCounts(
                reportRepository.countByStatus(ReportStatus.OPEN),
                reportRepository.countByStatus(ReportStatus.UNDER_REVIEW),
                reportRepository.countByStatus(ReportStatus.RESOLVED),
                reportRepository.countByStatus(ReportStatus.DISMISSED)
        );
    }

    public ReportSummaryView beginReview(UUID reportId, UUID adminId) {
        Report report = findReport(reportId);
        report.beginReview(adminId);
        return toSummaryView(reportRepository.save(report));
    }

    public ReportSummaryView resolve(UUID reportId, UUID adminId, String note) {
        Report report = findReport(reportId);
        report.resolve(adminId, note);
        ReportSummaryView view = toSummaryView(reportRepository.save(report));
        auditLogService.record(adminName(adminId), "REPORT", "Resolved report against \"" + view.target().label() + "\"");
        return view;
    }

    public ReportSummaryView dismiss(UUID reportId, UUID adminId, String note) {
        Report report = findReport(reportId);
        report.dismiss(adminId, note);
        ReportSummaryView view = toSummaryView(reportRepository.save(report));
        auditLogService.record(adminName(adminId), "REPORT", "Dismissed report against \"" + view.target().label() + "\"");
        return view;
    }

    private String adminName(UUID adminId) {
        return userRepository.findById(adminId).map(User::getFullName).orElse("Unknown admin");
    }

    private Report findReport(UUID id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
    }

    private ReportSummaryView toSummaryView(Report report) {
        ReporterSummary reporter = userRepository.findById(report.getReporterId())
                .map(u -> new ReporterSummary(u.getId(), u.getStudentNumber(), u.getFullName()))
                .orElse(new ReporterSummary(report.getReporterId(), "unknown", "Deleted account"));

        TargetSummary target = switch (report.getTargetType()) {
            case LISTING -> resolveListingTarget(report.getTargetId());
            case USER -> resolveUserTarget(report.getTargetId());
        };

        long totalReportsOnTarget = reportRepository.countByTargetTypeAndTargetId(report.getTargetType(), report.getTargetId());

        return ReportSummaryView.of(report, reporter, target, totalReportsOnTarget);
    }

    private TargetSummary resolveListingTarget(UUID listingId) {
        return listingRepository.findById(listingId)
                .map(listing -> new TargetSummary(
                        "LISTING", listing.getId(), listing.getName(), businessName(listing)))
                .orElse(new TargetSummary("LISTING", listingId, "Deleted listing", null));
    }

    private TargetSummary resolveUserTarget(UUID userId) {
        return userRepository.findById(userId)
                .map(user -> new TargetSummary(
                        "USER", user.getId(), user.getFullName(), "Student #" + user.getStudentNumber()))
                .orElse(new TargetSummary("USER", userId, "Deleted account", null));
    }

    private String businessName(Listing listing) {
        return businessRepository.findById(listing.getBusinessId())
                .map(Business::getBusinessName)
                .orElse("Unknown business");
    }
}
