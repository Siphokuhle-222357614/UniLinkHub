package za.co.unilinkhub.admin.application;

import za.co.unilinkhub.report.application.ReportStatusCounts;

public record AdminStatsDTO(
        long totalStudents,
        long pendingAccounts,
        BusinessCounts businesses,
        ListingCounts listings,
        ReportStatusCounts reports
) {
    public record BusinessCounts(long pending, long verified, long rejected) {
    }

    public record ListingCounts(long active, long inactive, long soldOut) {
    }
}
