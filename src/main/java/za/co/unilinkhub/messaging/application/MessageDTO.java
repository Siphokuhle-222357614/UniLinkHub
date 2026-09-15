package za.co.unilinkhub.messaging.application;

import za.co.unilinkhub.messaging.domain.Message;

import java.time.LocalDateTime;
import java.util.UUID;

public record MessageDTO(
        UUID id,
        UUID conversationId,
        UUID senderId,
        String body,
        boolean read,
        LocalDateTime createdAt
) {
    public static MessageDTO from(Message message) {
        return new MessageDTO(message.getId(), message.getConversationId(), message.getSenderId(),
                message.getBody(), message.isRead(), message.getCreatedAt());
    }
}
