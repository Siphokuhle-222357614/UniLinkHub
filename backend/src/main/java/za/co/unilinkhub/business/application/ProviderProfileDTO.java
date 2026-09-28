package za.co.unilinkhub.business.application;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * The public-facing view of a business: enough to build trust (who runs it, is it verified,
 * how active are they) without exposing the owner's contact details or student number -
 * that traceability exists for moderation (see the report flow), not public display.
 */
public record ProviderProfileDTO(
        UUID businessId,
        String businessName,
        String description,
        String category,
        String verificationStatus,
        String imageUrl,
        UUID ownerId,
        String ownerFullName,
        long activeListingCount,
        long totalViews,
        LocalDateTime memberSince
) {
}
