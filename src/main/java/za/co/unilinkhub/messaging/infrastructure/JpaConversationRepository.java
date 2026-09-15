package za.co.unilinkhub.messaging.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.unilinkhub.messaging.domain.Conversation;
import za.co.unilinkhub.messaging.repository.ConversationRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaConversationRepository extends JpaRepository<Conversation, UUID>, ConversationRepository {
    @Override
    Optional<Conversation> findByBusinessIdAndBuyerId(UUID businessId, UUID buyerId);

    @Override
    List<Conversation> findByBuyerId(UUID buyerId);

    @Override
    List<Conversation> findBySellerId(UUID sellerId);
}
