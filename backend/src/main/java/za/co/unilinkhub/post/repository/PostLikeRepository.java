package za.co.unilinkhub.post.repository;

import za.co.unilinkhub.post.domain.PostLike;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PostLikeRepository {

    PostLike save(PostLike like);

    boolean existsByPostIdAndUserId(UUID postId, UUID userId);

    void deleteByPostIdAndUserId(UUID postId, UUID userId);

    void deleteByPostId(UUID postId);

    List<PostLike> findByPostIdIn(Collection<UUID> postIds);
}
