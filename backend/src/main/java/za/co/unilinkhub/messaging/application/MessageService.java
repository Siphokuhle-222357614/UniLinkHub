package za.co.unilinkhub.messaging.application;

import za.co.unilinkhub.realtime.RealtimeHub;
import java.util.Map;
import za.co.unilinkhub.common.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import za.co.unilinkhub.business.domain.Business;
import za.co.unilinkhub.business.repository.BusinessRepository;
import za.co.unilinkhub.common.exception.BadRequestException;
import za.co.unilinkhub.common.exception.ResourceNotFoundException;
import za.co.unilinkhub.listing.domain.Listing;
import za.co.unilinkhub.listing.repository.ListingRepository;
import za.co.unilinkhub.messaging.domain.Conversation;
import za.co.unilinkhub.messaging.domain.Message;
import za.co.unilinkhub.messaging.repository.ConversationRepository;
import za.co.unilinkhub.messaging.repository.MessageRepository;
import za.co.unilinkhub.notification.application.NotificationService;
import za.co.unilinkhub.user.domain.User;
import za.co.unilinkhub.user.repository.UserRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ListingRepository listingRepository;
    private final BusinessRepository businessRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final RealtimeHub realtimeHub;

    public ConversationSummaryView startConversation(UUID buyerId, UUID listingId, String body) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("We couldn't find that listing. It may have been removed by the seller."));
        Business business = businessRepository.findById(listing.getBusinessId())
                .orElseThrow(() -> new ResourceNotFoundException("We couldn't find that business. It may have been removed."));
        if (business.getOwnerId().equals(buyerId)) {
            throw new BadRequestException("You can't message your own business - the conversation would only be with yourself.");
        }

        Conversation conversation = conversationRepository.findByBusinessIdAndBuyerId(business.getId(), buyerId)
                .orElseGet(() -> conversationRepository.save(
                        Conversation.start(business.getId(), buyerId, business.getOwnerId(), listingId)));

        Message first = messageRepository.save(Message.send(conversation.getId(), buyerId, body));
        realtimeHub.publish(business.getOwnerId(), "message",
                Map.of("conversationId", conversation.getId(), "message", MessageDTO.from(first)));

        String buyerName = userRepository.findById(buyerId).map(User::getFullName).orElse("A student");
        notificationService.notify(business.getOwnerId(), "MESSAGE",
                "New message from " + buyerName + " about \"" + listing.getName() + "\"");

        return toSummary(conversation, buyerId);
    }

    public MessageDTO reply(UUID conversationId, UUID senderId, String body) {
        Conversation conversation = findParticipant(conversationId, senderId);
        Message message = messageRepository.save(Message.send(conversationId, senderId, body));

        UUID recipientId = conversation.getBuyerId().equals(senderId) ? conversation.getSellerId() : conversation.getBuyerId();
        String senderName = conversation.getBuyerId().equals(senderId)
                ? userRepository.findById(senderId).map(User::getFullName).orElse("A student")
                : businessRepository.findById(conversation.getBusinessId()).map(Business::getBusinessName).orElse("A seller");
        notificationService.notify(recipientId, "MESSAGE", "New message from " + senderName);
        MessageDTO dto = MessageDTO.from(message);
        realtimeHub.publish(recipientId, "message", Map.of("conversationId", conversationId, "message", dto));
        return dto;
    }

    public List<ConversationSummaryView> listMine(UUID userId) {
        List<Conversation> mine = new ArrayList<>();
        mine.addAll(conversationRepository.findByBuyerId(userId));
        mine.addAll(conversationRepository.findBySellerId(userId));
        return mine.stream()
                .map(c -> toSummary(c, userId))
                .sorted(Comparator.comparing(ConversationSummaryView::lastMessageAt).reversed())
                .toList();
    }

    public List<MessageDTO> getMessages(UUID conversationId, UUID userId) {
        Conversation conversation = findParticipant(conversationId, userId);
        List<Message> messages = messageRepository.findByConversationId(conversation.getId());
        for (Message m : messages) {
            if (!m.getSenderId().equals(userId) && !m.isRead()) {
                m.markRead();
                messageRepository.save(m);
            }
        }
        return messages.stream().sorted(Comparator.comparing(Message::getCreatedAt)).map(MessageDTO::from).toList();
    }

    public long unreadCount(UUID userId) {
        List<Conversation> mine = new ArrayList<>();
        mine.addAll(conversationRepository.findByBuyerId(userId));
        mine.addAll(conversationRepository.findBySellerId(userId));
        return mine.stream().mapToLong(c -> messageRepository.countByConversationIdAndSenderIdNotAndReadFalse(c.getId(), userId)).sum();
    }

    private Conversation findParticipant(UUID conversationId, UUID userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("We couldn't find that conversation."));
        if (!conversation.getBuyerId().equals(userId) && !conversation.getSellerId().equals(userId)) {
            throw new ForbiddenException("You can only open conversations you're part of. Messages are private between the buyer and the seller.");
        }
        return conversation;
    }

    private ConversationSummaryView toSummary(Conversation conversation, UUID viewerId) {
        boolean viewerIsBuyer = conversation.getBuyerId().equals(viewerId);
        String businessName = businessRepository.findById(conversation.getBusinessId())
                .map(Business::getBusinessName).orElse("Unknown business");
        UUID counterpartId = viewerIsBuyer ? conversation.getSellerId() : conversation.getBuyerId();
        String counterpartName = viewerIsBuyer
                ? businessName
                : userRepository.findById(conversation.getBuyerId()).map(User::getFullName).orElse("Deleted account");
        String listingName = conversation.getListingId() == null ? null
                : listingRepository.findById(conversation.getListingId()).map(Listing::getName).orElse(null);

        List<Message> messages = messageRepository.findByConversationId(conversation.getId());
        Message last = messages.stream().max(Comparator.comparing(Message::getCreatedAt)).orElse(null);
        long unread = messageRepository.countByConversationIdAndSenderIdNotAndReadFalse(conversation.getId(), viewerId);

        return new ConversationSummaryView(
                conversation.getId(), conversation.getBusinessId(), businessName,
                conversation.getListingId(), listingName, counterpartId, counterpartName, !viewerIsBuyer,
                last == null ? "" : last.getBody(), last == null ? conversation.getCreatedAt() : last.getCreatedAt(), unread
        );
    }
}
