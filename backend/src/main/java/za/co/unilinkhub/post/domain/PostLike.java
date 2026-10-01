package za.co.unilinkhub.post.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/** One student liking one post - the unique constraint makes a second like impossible, even under a double-click. */
@Entity
@Table(name = "post_likes", uniqueConstraints = @UniqueConstraint(name = "uk_post_likes", columnNames = {"post_id", "user_id"}))
@Getter
@NoArgsConstructor
public class PostLike {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "post_id", nullable = false)
    private UUID postId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    // Set in the factory rather than by @CreationTimestamp: that only fills the field when the row is
    // flushed, after the API response has been built, so a just-created one went out with no date.
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static PostLike of(UUID postId, UUID userId) {
        PostLike like = new PostLike();
        like.postId = postId;
        like.userId = userId;
        like.createdAt = LocalDateTime.now();
        return like;
    }
}
