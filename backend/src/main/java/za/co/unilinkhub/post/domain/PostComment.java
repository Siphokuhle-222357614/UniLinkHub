package za.co.unilinkhub.post.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/** A student's comment on a business post. The page owner, the author and admins can delete it. */
@Entity
@Table(name = "post_comments")
@Getter
@NoArgsConstructor
public class PostComment {

    public static final int MAX_LENGTH = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "post_id", nullable = false)
    private UUID postId;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Column(nullable = false, length = MAX_LENGTH)
    private String body;

    // Set in the factory rather than by @CreationTimestamp: that only fills the field when the row is
    // flushed, after the API response has been built, so a just-created one went out with no date.
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static PostComment of(UUID postId, UUID authorId, String body) {
        PostComment comment = new PostComment();
        comment.postId = postId;
        comment.authorId = authorId;
        comment.body = body;
        comment.createdAt = LocalDateTime.now();
        return comment;
    }
}
