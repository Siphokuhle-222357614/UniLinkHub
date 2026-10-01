package za.co.unilinkhub.trust.application;

import java.util.UUID;

/**
 * The signals that help a student decide whether to buy from someone they've never met:
 * how they're rated, how many orders they've completed, and how quickly they reply.
 *
 * @param rating            average review rating to one decimal, or null with no reviews yet
 * @param responseTimeLabel e.g. "within an hour"; null until there are enough conversations to say
 */
public record SellerTrust(
        UUID businessId,
        boolean verified,
        Double rating,
        long reviewCount,
        long completedOrders,
        String responseTimeLabel
) {
}
