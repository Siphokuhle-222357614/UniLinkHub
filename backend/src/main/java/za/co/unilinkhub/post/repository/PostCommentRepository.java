package za.co.unilinkhub.post.repository;

import za.co.unilinkhub.post.domain.PostComment;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PostCommentRepository {

    PostComment save(PostComment comment);

    Optional<PostComment> findById(UUID id);

    List<PostComment> findByPostIdOrderByCreatedAtAsc(UUID postId);

    List<PostComment> findByPostIdIn(Collection<UUID> postIds);

    void delete(PostComment comment);

    void deleteByPostId(UUID postId);
}
