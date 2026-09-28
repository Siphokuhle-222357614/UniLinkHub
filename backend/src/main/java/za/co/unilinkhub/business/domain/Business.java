package za.co.unilinkhub.business.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A student-run business. References its owner by id (rather than embedding the User aggregate)
 * to keep the user and business bounded contexts decoupled in this modular-monolith layout.
 */
@Entity
@Table(name = "businesses")
@Getter
@NoArgsConstructor
public class Business {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "business_name", nullable = false, length = 150)
    private String businessName;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(nullable = false, length = 100)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 20)
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private Business(UUID ownerId, String businessName, String description, String category) {
        this.ownerId = ownerId;
        this.businessName = businessName;
        this.description = description;
        this.category = category;
    }

    public static Business create(UUID ownerId, String businessName, String description, String category) {
        return new Business(ownerId, businessName, description, category);
    }

    public void updateDetails(String businessName, String description, String category, String imageUrl) {
        if (businessName != null && !businessName.isBlank()) {
            this.businessName = businessName;
        }
        if (description != null && !description.isBlank()) {
            this.description = description;
        }
        if (category != null && !category.isBlank()) {
            this.category = category;
        }
        if (imageUrl != null) {
            this.imageUrl = imageUrl.isBlank() ? null : imageUrl;
        }
    }

    public void requestVerification() {
        if (this.verificationStatus == VerificationStatus.VERIFIED) {
            throw new IllegalStateException("Business is already verified");
        }
        this.verificationStatus = VerificationStatus.PENDING;
        this.rejectionReason = null;
    }

    public void verify() {
        this.verificationStatus = VerificationStatus.VERIFIED;
        this.rejectionReason = null;
    }

    public void reject(String reason) {
        this.verificationStatus = VerificationStatus.REJECTED;
        this.rejectionReason = reason;
    }
}
