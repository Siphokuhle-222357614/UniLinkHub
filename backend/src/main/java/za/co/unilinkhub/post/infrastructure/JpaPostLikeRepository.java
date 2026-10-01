package za.co.unilinkhub.post.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.unilinkhub.post.domain.PostLike;
import za.co.unilinkhub.post.repository.PostLikeRepository;

import java.util.UUID;

public interface JpaPostLikeRepository extends JpaRepository<PostLike, UUID>, PostLikeRepository {
}
