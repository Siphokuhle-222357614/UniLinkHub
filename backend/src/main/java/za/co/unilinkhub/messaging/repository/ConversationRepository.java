package za.co.unilinkhub.messaging.repository;

import za.co.unilinkhub.messaging.domain.Conversation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository {

    Conversation save(Conversation conversation);

    Optional<Conversation> findById(UUID id);

    Optional<Conversation> findByBusinessIdAndBuyerId(UUID businessId, UUID buyerId);

    List<Conversation> findByBuyerId(UUID buyerId);

    List<Conversation> findBySellerId(UUID sellerId);

    List<Conversation> findByBusinessId(UUID businessId);
}
