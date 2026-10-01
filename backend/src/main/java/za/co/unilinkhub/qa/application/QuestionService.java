package za.co.unilinkhub.qa.application;

import za.co.unilinkhub.common.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import za.co.unilinkhub.audit.application.AuditLogService;
import za.co.unilinkhub.business.domain.Business;
import za.co.unilinkhub.business.repository.BusinessRepository;
import za.co.unilinkhub.common.exception.ResourceNotFoundException;
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
    private final AuditLogService auditLogService;

    public QuestionView ask(UUID askerId, UUID listingId, String questionText) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("We couldn't find that listing. It may have been removed by the seller."));
        Business business = businessRepository.findById(listing.getBusinessId())
                .orElseThrow(() -> new ResourceNotFoundException("We couldn't find that business. It may have been removed."));
        if (business.getOwnerId().equals(askerId)) {
            throw new ForbiddenException("You can't ask a question on your own listing. To add details, edit the listing's description instead.");
        }

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
        List<UUID> listingIds = sellerListingIds(sellerId);
        if (listingIds.isEmpty()) {
            return List.of();
        }
        return questionRepository.findByAnswerTextIsNullAndListingIdIn(listingIds).stream()
                .sorted(Comparator.comparing(Question::getCreatedAt))
                .map(this::toView)
                .toList();
    }

    /**
     * Every question across a seller's listings, answered or not - the dashboard widget only
     * shows what's pending, but a seller reviewing their own Q&A history needs the full picture.
     */
    public List<QuestionView> listForSeller(UUID sellerId) {
        List<UUID> listingIds = sellerListingIds(sellerId);
        if (listingIds.isEmpty()) {
            return List.of();
        }
        return questionRepository.findByListingIdIn(listingIds).stream()
                .sorted(Comparator.comparing(Question::getCreatedAt).reversed())
                .map(this::toView)
                .toList();
    }

    public void flag(UUID questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("We couldn't find that question. It may have been removed."));
        question.flag();
        questionRepository.save(question);
    }

    public List<QuestionView> adminList(boolean flaggedOnly) {
        List<Question> questions = flaggedOnly ? questionRepository.findByFlaggedTrue() : questionRepository.findAll();
        return questions.stream()
                .sorted(Comparator.comparing(Question::getCreatedAt).reversed())
                .map(this::toView)
                .toList();
    }

    public void adminRemove(UUID questionId, UUID adminId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("We couldn't find that question. It may have been removed."));
        String listingName = listingRepository.findById(question.getListingId()).map(Listing::getName).orElse("a deleted listing");
        questionRepository.deleteById(questionId);
        String adminName = userRepository.findById(adminId).map(User::getFullName).orElse("Unknown admin");
        auditLogService.record(adminName, "QUESTION", "Removed a question on \"" + listingName + "\"");
    }

    public QuestionStatsDTO adminStats() {
        List<Question> all = questionRepository.findAll();
        long answered = all.stream().filter(q -> q.getAnswerText() != null).count();
        long flagged = all.stream().filter(Question::isFlagged).count();
        return new QuestionStatsDTO(all.size(), answered, all.size() - answered, flagged);
    }

    private List<UUID> sellerListingIds(UUID sellerId) {
        return businessRepository.findByOwnerId(sellerId).stream()
                .map(Business::getId)
                .flatMap(businessId -> listingRepository.findByBusinessId(businessId).stream())
                .map(Listing::getId)
                .toList();
    }

    private Question findOwned(UUID questionId, UUID sellerId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("We couldn't find that question. It may have been removed."));
        Listing listing = listingRepository.findById(question.getListingId())
                .orElseThrow(() -> new ResourceNotFoundException("We couldn't find that listing. It may have been removed by the seller."));
        Business business = businessRepository.findById(listing.getBusinessId())
                .orElseThrow(() -> new ResourceNotFoundException("We couldn't find that business. It may have been removed."));
        if (!business.getOwnerId().equals(sellerId)) {
            throw new ForbiddenException("Only the owner of this business can do this. You can only manage businesses you created yourself.");
        }
        return question;
    }

    private QuestionView toView(Question question) {
        String listingName = listingRepository.findById(question.getListingId()).map(Listing::getName).orElse("Deleted listing");
        String askerName = userRepository.findById(question.getAskerId()).map(User::getFullName).orElse("Deleted account");
        return new QuestionView(question.getId(), question.getListingId(), listingName, question.getAskerId(), askerName,
                question.getQuestionText(), question.getAnswerText(), question.getAnsweredAt(),
                question.isFlagged(), question.getFlagCount(), question.getCreatedAt());
    }
}
