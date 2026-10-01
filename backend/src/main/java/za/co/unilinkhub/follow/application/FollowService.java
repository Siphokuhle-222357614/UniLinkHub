package za.co.unilinkhub.follow.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.unilinkhub.business.application.BusinessService;
import za.co.unilinkhub.business.application.ProviderProfileDTO;
import za.co.unilinkhub.business.repository.BusinessRepository;
import za.co.unilinkhub.business.domain.Business;
import za.co.unilinkhub.common.exception.ForbiddenException;
import za.co.unilinkhub.common.exception.ResourceNotFoundException;
import za.co.unilinkhub.follow.domain.FollowedBusiness;
import za.co.unilinkhub.follow.repository.FollowedBusinessRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FollowService {

    private final FollowedBusinessRepository followedBusinessRepository;
    private final BusinessRepository businessRepository;
    private final BusinessService businessService;

    public void follow(UUID userId, UUID businessId) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("We couldn't find that business. It may have been removed."));
        if (business.getOwnerId().equals(userId)) {
            throw new ForbiddenException("You can't follow your own business. Followers are the students who want your updates - "
                    + "share a post on your page to give them a reason to follow.");
        }
        if (!followedBusinessRepository.existsByUserIdAndBusinessId(userId, businessId)) {
            followedBusinessRepository.save(FollowedBusiness.create(userId, businessId));
        }
    }

    @Transactional
    public void unfollow(UUID userId, UUID businessId) {
        followedBusinessRepository.deleteByUserIdAndBusinessId(userId, businessId);
    }

    public boolean isFollowing(UUID userId, UUID businessId) {
        return followedBusinessRepository.existsByUserIdAndBusinessId(userId, businessId);
    }

    public List<ProviderProfileDTO> mine(UUID userId) {
        return followedBusinessRepository.findByUserId(userId).stream()
                .sorted(Comparator.comparing(FollowedBusiness::getCreatedAt).reversed())
                .map(FollowedBusiness::getBusinessId)
                .map(this::safeProfile)
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<ProviderProfileDTO> safeProfile(UUID businessId) {
        try {
            return Optional.of(businessService.getProviderProfile(businessId));
        } catch (ResourceNotFoundException ex) {
            return Optional.empty();
        }
    }
}
