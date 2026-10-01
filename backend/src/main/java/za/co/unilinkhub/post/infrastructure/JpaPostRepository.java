package za.co.unilinkhub.post.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.unilinkhub.post.domain.BusinessPost;
import za.co.unilinkhub.post.repository.PostRepository;

import java.util.UUID;

public interface JpaPostRepository extends JpaRepository<BusinessPost, UUID>, PostRepository {
}
