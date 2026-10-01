package za.co.unilinkhub.trust.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import za.co.unilinkhub.business.domain.Business;
import za.co.unilinkhub.business.domain.VerificationStatus;
import za.co.unilinkhub.business.repository.BusinessRepository;
import za.co.unilinkhub.messaging.domain.Conversation;
import za.co.unilinkhub.messaging.domain.Message;
import za.co.unilinkhub.messaging.repository.ConversationRepository;
import za.co.unilinkhub.messaging.repository.MessageRepository;
import za.co.unilinkhub.ordering.domain.OrderStatus;
import za.co.unilinkhub.ordering.repository.OrderRepository;
import za.co.unilinkhub.review.domain.Review;
import za.co.unilinkhub.review.repository.ReviewRepository;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Works out {@link SellerTrust} for a business. These are shown on every listing card, so results
 * are cached for a few minutes - a rating that's five minutes stale is fine, a slow browse page isn't.
 */
@Service
@RequiredArgsConstructor
public class SellerTrustService {

    private static final Duration CACHE_FOR = Duration.ofMinutes(5);
    private static final Duration RESPONSE_WINDOW = Duration.ofDays(90);
    /** Fewer than this many answered conversations isn't enough to call a reply time "usual". */
    private static final int MIN_CONVERSATIONS = 2;

    private final BusinessRepository businessRepository;
    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    private record Cached(SellerTrust value, Instant at) {
    }

    private final Map<UUID, Cached> cache = new ConcurrentHashMap<>();

    public SellerTrust forBusiness(UUID businessId) {
        Cached hit = cache.get(businessId);
        if (hit != null && hit.at().plus(CACHE_FOR).isAfter(Instant.now())) {
            return hit.value();
        }
        SellerTrust fresh = compute(businessId);
        cache.put(businessId, new Cached(fresh, Instant.now()));
        return fresh;
    }

    public Map<UUID, SellerTrust> forBusinesses(Collection<UUID> businessIds) {
        Map<UUID, SellerTrust> result = new HashMap<>();
        for (UUID id : Set.copyOf(businessIds)) {
            result.put(id, forBusiness(id));
        }
        return result;
    }

    private SellerTrust compute(UUID businessId) {
        Business business = businessRepository.findById(businessId).orElse(null);
        boolean verified = business != null && business.getVerificationStatus() == VerificationStatus.VERIFIED;

        List<Review> reviews = reviewRepository.findByBusinessId(businessId);
        Double rating = reviews.isEmpty() ? null
                : Math.round(reviews.stream().mapToInt(Review::getRating).average().orElse(0) * 10) / 10.0;

        long completed = orderRepository.countByBusinessIdAndStatus(businessId, OrderStatus.COMPLETED);
        return new SellerTrust(businessId, verified, rating, reviews.size(), completed, responseTimeLabel(businessId));
    }

    /**
     * The median time between a buyer's first message in a conversation and the seller's first
     * reply after it, over the last 90 days. The median (not the average) so one weekend away
     * doesn't make an otherwise quick seller look slow.
     */
    private String responseTimeLabel(UUID businessId) {
        LocalDateTime since = LocalDateTime.now().minus(RESPONSE_WINDOW);
        List<Conversation> conversations = conversationRepository.findByBusinessId(businessId);
        if (conversations.isEmpty()) {
            return null;
        }
        Map<UUID, Conversation> byId = conversations.stream().collect(Collectors.toMap(Conversation::getId, c -> c));
        Map<UUID, List<Message>> messages = messageRepository.findByConversationIdIn(byId.keySet()).stream()
                .collect(Collectors.groupingBy(Message::getConversationId));

        List<Long> minutes = messages.entrySet().stream().map(e -> {
            Conversation c = byId.get(e.getKey());
            List<Message> sorted = e.getValue().stream().sorted(Comparator.comparing(Message::getCreatedAt)).toList();
            Message firstFromBuyer = sorted.stream().filter(m -> m.getSenderId().equals(c.getBuyerId())).findFirst().orElse(null);
            if (firstFromBuyer == null || firstFromBuyer.getCreatedAt().isBefore(since)) {
                return null;
            }
            return sorted.stream()
                    .filter(m -> m.getSenderId().equals(c.getSellerId()) && m.getCreatedAt().isAfter(firstFromBuyer.getCreatedAt()))
                    .findFirst()
                    .map(reply -> Duration.between(firstFromBuyer.getCreatedAt(), reply.getCreatedAt()).toMinutes())
                    .orElse(null);
        }).filter(Objects::nonNull).sorted().toList();

        if (minutes.size() < MIN_CONVERSATIONS) {
            return null;
        }
        long median = minutes.get(minutes.size() / 2);
        if (median <= 60) {
            return "within an hour";
        }
        if (median <= 6 * 60) {
            return "within a few hours";
        }
        if (median <= 24 * 60) {
            return "within a day";
        }
        return "in a few days";
    }
}
