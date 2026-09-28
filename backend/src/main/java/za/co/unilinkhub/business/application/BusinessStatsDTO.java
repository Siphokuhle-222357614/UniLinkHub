package za.co.unilinkhub.business.application;

public record BusinessStatsDTO(
        long totalListings,
        long activeListings,
        long totalViews,
        long totalSaves,
        long followerCount
) {
}
