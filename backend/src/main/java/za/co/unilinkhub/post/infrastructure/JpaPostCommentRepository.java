package za.co.unilinkhub.post.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.unilinkhub.post.domain.PostComment;
import za.co.unilinkhub.post.repository.PostCommentRepository;

import java.util.UUID;

public interface JpaPostCommentRepository extends JpaRepository<PostComment, UUID>, PostCommentRepository {
}
