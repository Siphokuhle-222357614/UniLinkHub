package za.co.unilinkhub.post.repository;

import za.co.unilinkhub.post.domain.BusinessPost;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PostRepository {

    BusinessPost save(BusinessPost post);

    Optional<BusinessPost> findById(UUID id);

    List<BusinessPost> findByBusinessId(UUID businessId);

    List<BusinessPost> findByBusinessIdIn(Collection<UUID> businessIds);

    List<BusinessPost> findAll();

    long countByBusinessIdAndCreatedAtAfter(UUID businessId, LocalDateTime since);

    void delete(BusinessPost post);
}
