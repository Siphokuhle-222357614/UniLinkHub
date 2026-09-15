package za.co.unilinkhub.qa.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.unilinkhub.qa.domain.Question;
import za.co.unilinkhub.qa.repository.QuestionRepository;

import java.util.List;
import java.util.UUID;

public interface JpaQuestionRepository extends JpaRepository<Question, UUID>, QuestionRepository {
    @Override
    List<Question> findByListingId(UUID listingId);

    @Override
    List<Question> findByAnswerTextIsNullAndListingIdIn(List<UUID> listingIds);
}
