package za.co.unilinkhub.messaging.application;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConversationSummaryView(
        UUID id,
        UUID businessId,
        String businessName,
        UUID listingId,
        String listingName,
        UUID counterpartId,
        String counterpartName,
        boolean iAmSeller,
        String lastMessage,
        LocalDateTime lastMessageAt,
        long unreadCount
) {
}
