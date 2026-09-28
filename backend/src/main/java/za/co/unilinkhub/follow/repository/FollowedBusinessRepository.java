package za.co.unilinkhub.follow.repository;

import za.co.unilinkhub.follow.domain.FollowedBusiness;

import java.util.List;
import java.util.UUID;

public interface FollowedBusinessRepository {

    FollowedBusiness save(FollowedBusiness followedBusiness);

    boolean existsByUserIdAndBusinessId(UUID userId, UUID businessId);

    void deleteByUserIdAndBusinessId(UUID userId, UUID businessId);

    List<FollowedBusiness> findByUserId(UUID userId);

    long countByBusinessId(UUID businessId);
}
