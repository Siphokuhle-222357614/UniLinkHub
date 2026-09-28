package za.co.unilinkhub.qa.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A public question-and-answer pair on a listing, visible to every visitor rather than a
 * private message - so the same answer helps everyone considering the same listing.
 */
@Entity
@Table(name = "questions")
@Getter
@NoArgsConstructor
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "listing_id", nullable = false)
    private UUID listingId;

    @Column(name = "asker_id", nullable = false)
    private UUID askerId;

    @Column(name = "question_text", nullable = false, length = 1000)
    private String questionText;

    @Column(name = "answer_text", length = 1000)
    private String answerText;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    @Column(nullable = false)
    private boolean flagged = false;

    @Column(name = "flag_count", nullable = false)
    private int flagCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private Question(UUID listingId, UUID askerId, String questionText) {
        this.listingId = listingId;
        this.askerId = askerId;
        this.questionText = questionText;
    }

    public static Question ask(UUID listingId, UUID askerId, String questionText) {
        return new Question(listingId, askerId, questionText);
    }

    public void answer(String answerText) {
        this.answerText = answerText;
        this.answeredAt = LocalDateTime.now();
    }

    public void flag() {
        this.flagCount++;
        this.flagged = true;
    }
}
