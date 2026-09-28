package za.co.unilinkhub.review.application;

import java.util.List;

public record ReviewStatsDTO(
        double platformAverage,
        long totalReviews,
        long flaggedCount,
        long reviewedBusinessCount,
        List<BusinessRating> topRated,
        List<BusinessRating> lowestRated
) {
    public record BusinessRating(String businessName, double average, long count) {
    }
}
