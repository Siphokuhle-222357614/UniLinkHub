package za.co.unilinkhub.post.application;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A post as the reader sees it. {@code canManage} is true for the business owner (edit, pin,
 * delete, remove comments); {@code removedReason} is only ever filled in for the owner and admins.
 */
public record PostView(
        UUID id,
        UUID businessId,
        String businessName,
        String businessImageUrl,
        boolean businessVerified,
        String body,
        String imageUrl,
        AttachedListing listing,
        boolean pinned,
        boolean edited,
        LocalDateTime createdAt,
        long likeCount,
        boolean likedByMe,
        long commentCount,
        boolean canManage,
        int flagCount,
        String removedReason
) {

    public record AttachedListing(UUID id, String name, BigDecimal price, String imageUrl, String category, String status) {
    }
}
