package za.co.unilinkhub.post.application;

import java.time.LocalDateTime;
import java.util.UUID;

/** {@code canDelete}: the comment's author, the page's owner, or an admin. */
public record CommentView(UUID id, UUID postId, UUID authorId, String authorName, String body,
                          LocalDateTime createdAt, boolean canDelete) {
}
