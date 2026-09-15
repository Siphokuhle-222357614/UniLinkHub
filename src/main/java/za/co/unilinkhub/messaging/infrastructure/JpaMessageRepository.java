package za.co.unilinkhub.messaging.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.unilinkhub.messaging.domain.Message;
import za.co.unilinkhub.messaging.repository.MessageRepository;

import java.util.List;
import java.util.UUID;

public interface JpaMessageRepository extends JpaRepository<Message, UUID>, MessageRepository {
    @Override
    List<Message> findByConversationId(UUID conversationId);

    @Override
    long countByConversationIdAndSenderIdNotAndReadFalse(UUID conversationId, UUID senderId);
}
