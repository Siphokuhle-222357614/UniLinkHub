package za.co.unilinkhub.business.application;

import za.co.unilinkhub.business.domain.Business;
import za.co.unilinkhub.user.application.UserDTO;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Business + resolved owner info, for the admin verification queue. Kept separate from
 * BusinessDTO so the plain owner-facing endpoints don't pay for a cross-module user lookup
 * they don't need.
 */
public record AdminBusinessView(
        UUID id,
        String businessName,
        String description,
        String category,
        String verificationStatus,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String ownerStudentNumber,
        String ownerFullName
) {
    public static AdminBusinessView from(Business business, UserDTO owner) {
        return new AdminBusinessView(
                business.getId(), business.getBusinessName(), business.getDescription(),
                business.getCategory(), business.getVerificationStatus().name(),
                business.getCreatedAt(), business.getUpdatedAt(),
                owner.studentNumber(), owner.firstName() + " " + owner.lastName()
        );
    }
}
