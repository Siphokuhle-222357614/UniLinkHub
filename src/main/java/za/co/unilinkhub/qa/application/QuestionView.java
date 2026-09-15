package za.co.unilinkhub.qa.application;

import java.time.LocalDateTime;
import java.util.UUID;

public record QuestionView(
        UUID id,
        UUID listingId,
        String listingName,
        UUID askerId,
        String askerName,
        String questionText,
        String answerText,
        LocalDateTime answeredAt,
        LocalDateTime createdAt
) {
}
