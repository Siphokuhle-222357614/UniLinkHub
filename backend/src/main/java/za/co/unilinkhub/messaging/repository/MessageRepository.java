package za.co.unilinkhub.messaging.repository;

import za.co.unilinkhub.messaging.domain.Message;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface MessageRepository {

    Message save(Message message);

    List<Message> findByConversationId(UUID conversationId);

    List<Message> findByConversationIdIn(Collection<UUID> conversationIds);

    long countByConversationIdAndSenderIdNotAndReadFalse(UUID conversationId, UUID senderId);
}
