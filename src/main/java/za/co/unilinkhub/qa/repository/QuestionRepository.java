package za.co.unilinkhub.qa.repository;

import za.co.unilinkhub.qa.domain.Question;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuestionRepository {

    Question save(Question question);

    Optional<Question> findById(UUID id);

    List<Question> findByListingId(UUID listingId);

    List<Question> findByAnswerTextIsNullAndListingIdIn(List<UUID> listingIds);
}
