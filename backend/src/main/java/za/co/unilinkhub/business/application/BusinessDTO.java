package za.co.unilinkhub.business.application;

import za.co.unilinkhub.business.domain.Business;

import java.time.LocalDateTime;
import java.util.UUID;

public record BusinessDTO(
        UUID id,
        UUID ownerId,
        String businessName,
        String description,
        String category,
        String verificationStatus,
        String imageUrl,
        String rejectionReason,
        LocalDateTime createdAt
) {
    public static BusinessDTO from(Business business) {
        return new BusinessDTO(
                business.getId(), business.getOwnerId(), business.getBusinessName(),
                business.getDescription(), business.getCategory(),
                business.getVerificationStatus().name(), business.getImageUrl(),
                business.getRejectionReason(), business.getCreatedAt()
        );
    }
}
