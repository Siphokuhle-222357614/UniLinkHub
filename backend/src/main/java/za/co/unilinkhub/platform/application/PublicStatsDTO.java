package za.co.unilinkhub.platform.application;

/**
 * Headline numbers for the public landing page. Only aggregate counts - nothing here identifies a
 * student or business, which is why the endpoint serving it needs no authentication.
 */
public record PublicStatsDTO(
        long students,
        long verifiedBusinesses,
        long activeListings,
        long completedOrders
) {
}
