package za.co.unilinkhub.report.application;

public record ReportStatusCounts(long open, long underReview, long resolved, long dismissed) {
    public long total() {
        return open + underReview + resolved + dismissed;
    }
}
