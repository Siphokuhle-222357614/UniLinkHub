package za.co.unilinkhub.qa.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import za.co.unilinkhub.business.domain.Business;
import za.co.unilinkhub.business.repository.BusinessRepository;
import za.co.unilinkhub.common.exception.ResourceNotFoundException;
import za.co.unilinkhub.common.exception.UnauthorizedException;
import za.co.unilinkhub.listing.domain.Listing;
import za.co.unilinkhub.listing.repository.ListingRepository;
import za.co.unilinkhub.notification.application.NotificationService;
import za.co.unilinkhub.qa.domain.Question;
import za.co.unilinkhub.qa.repository.QuestionRepository;
import za.co.unilinkhub.user.domain.User;
import za.co.unilinkhub.user.repository.UserRepository;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final ListingRepository listingRepository;
    private final BusinessRepository businessRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public QuestionView ask(UUID askerId, UUID listingId, String questionText) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));
        Business business = businessRepository.findById(listing.getBusinessId())
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));

        Question question = questionRepository.save(Question.ask(listingId, askerId, questionText));
        notificationService.notify(business.getOwnerId(), "QUESTION", "New question on \"" + listing.getName() + "\"");
        return toView(question);
    }

    public List<QuestionView> listForListing(UUID listingId) {
        return questionRepository.findByListingId(listingId).stream()
                .sorted(Comparator.comparing(Question::getCreatedAt).reversed())
                .map(this::toView)
                .toList();
    }

    public QuestionView answer(UUID questionId, UUID sellerId, String answerText) {
        Question question = findOwned(questionId, sellerId);
        question.answer(answerText);
        Question saved = questionRepository.save(question);

        String listingName = listingRepository.findById(question.getListingId()).map(Listing::getName).orElse("your question");
        notificationService.notify(question.getAskerId(), "QUESTION", "Your question was answered on \"" + listingName + "\"");
        return toView(saved);
    }

    public List<QuestionView> pendingForSeller(UUID sellerId) {
        List<UUID> listingIds = businessRepository.findByOwnerId(sellerId).stream()
                .map(Business::getId)
                .flatMap(businessId -> listingRepository.findByBusinessId(businessId).stream())
                .map(Listing::getId)
                .toList();
        if (listingIds.isEmpty()) {
            return List.of();
        }
        return questionRepository.findByAnswerTextIsNullAndListingIdIn(listingIds).stream()
                .sorted(Comparator.comparing(Question::getCreatedAt))
                .map(this::toView)
                .toList();
    }

    private Question findOwned(UUID questionId, UUID sellerId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found"));
        Listing listing = listingRepository.findById(question.getListingId())
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));
        Business business = businessRepository.findById(listing.getBusinessId())
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));
        if (!business.getOwnerId().equals(sellerId)) {
            throw new UnauthorizedException("You do not own this business");
        }
        return question;
    }

    private QuestionView toView(Question question) {
        String listingName = listingRepository.findById(question.getListingId()).map(Listing::getName).orElse("Deleted listing");
        String askerName = userRepository.findById(question.getAskerId()).map(User::getFullName).orElse("Deleted account");
        return new QuestionView(question.getId(), question.getListingId(), listingName, question.getAskerId(), askerName,
                question.getQuestionText(), question.getAnswerText(), question.getAnsweredAt(), question.getCreatedAt());
    }
}
