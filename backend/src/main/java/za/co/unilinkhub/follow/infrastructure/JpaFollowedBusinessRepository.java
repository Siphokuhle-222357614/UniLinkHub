package za.co.unilinkhub.follow.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.unilinkhub.follow.domain.FollowedBusiness;
import za.co.unilinkhub.follow.repository.FollowedBusinessRepository;

import java.util.List;
import java.util.UUID;

public interface JpaFollowedBusinessRepository extends JpaRepository<FollowedBusiness, UUID>, FollowedBusinessRepository {

    @Override
    boolean existsByUserIdAndBusinessId(UUID userId, UUID businessId);

    @Override
    void deleteByUserIdAndBusinessId(UUID userId, UUID businessId);

    @Override
    List<FollowedBusiness> findByUserId(UUID userId);

    @Override
    long countByBusinessId(UUID businessId);
}
