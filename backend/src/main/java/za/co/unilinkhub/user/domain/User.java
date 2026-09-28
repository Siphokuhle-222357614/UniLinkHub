package za.co.unilinkhub.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A verified member of the university community. Every user starts as a buyer; calling
 * {@link #becomeSeller()} unlocks listing creation on the same account (Section 13.2 of the
 * project docs) rather than requiring a second, separate seller account.
 */
@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_email", columnNames = "email"),
        @UniqueConstraint(name = "uk_users_student_number", columnNames = "student_number")
})
@Getter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "student_number", nullable = false, length = 32)
    private String studentNumber;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "phone_number", length = 32)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role = UserRole.STUDENT;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false, length = 20)
    private AccountStatus accountStatus = AccountStatus.PENDING_VERIFICATION;

    @Column(name = "is_seller", nullable = false)
    private boolean seller = false;

    @Column(name = "verification_token", length = 64)
    private String verificationToken;

    @Column(name = "password_reset_token", length = 64)
    private String passwordResetToken;

    @Column(name = "pending_email", length = 254)
    private String pendingEmail;

    @Column(name = "email_change_token", length = 64)
    private String emailChangeToken;

    @Column(name = "suspension_reason", length = 1000)
    private String suspensionReason;

    /**
     * Comma-separated notification categories this user has turned OFF (e.g. "ANNOUNCEMENT").
     * Null/blank means nothing is disabled - opt-out rather than opt-in, so a category added
     * later defaults to enabled for everyone rather than silently going to no one.
     */
    @Column(name = "disabled_notification_categories", length = 200)
    private String disabledNotificationCategories;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private User(String studentNumber, String firstName, String lastName, String email,
                  String passwordHash, String verificationToken) {
        this.studentNumber = studentNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.verificationToken = verificationToken;
    }

    public static User register(String studentNumber, String firstName, String lastName,
                                 String email, String passwordHash, String verificationToken) {
        return new User(studentNumber, firstName, lastName, email, passwordHash, verificationToken);
    }

    public void verifyEmail(String token) {
        if (this.accountStatus != AccountStatus.PENDING_VERIFICATION) {
            throw new IllegalStateException("Account is not pending verification");
        }
        if (this.verificationToken == null || !this.verificationToken.equals(token)) {
            throw new IllegalArgumentException("Invalid verification token");
        }
        this.accountStatus = AccountStatus.ACTIVE;
        this.verificationToken = null;
    }

    public void approve() {
        if (this.accountStatus != AccountStatus.PENDING_VERIFICATION) {
            throw new IllegalStateException("Account is not pending verification");
        }
        this.accountStatus = AccountStatus.ACTIVE;
        this.verificationToken = null;
    }

    public void becomeSeller() {
        this.seller = true;
    }

    public void suspend(String reason) {
        this.accountStatus = AccountStatus.SUSPENDED;
        this.suspensionReason = reason;
    }

    public void reactivate() {
        this.accountStatus = AccountStatus.ACTIVE;
        this.suspensionReason = null;
    }

    public void deactivate() {
        this.accountStatus = AccountStatus.DEACTIVATED;
    }

    public void promoteToAdmin() {
        this.role = UserRole.ADMIN;
    }

    public void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
    }

    public void requestPasswordReset(String token) {
        this.passwordResetToken = token;
    }

    public void resetPassword(String token, String newPasswordHash) {
        if (this.passwordResetToken == null || !this.passwordResetToken.equals(token)) {
            throw new IllegalArgumentException("Invalid or expired reset code");
        }
        this.passwordHash = newPasswordHash;
        this.passwordResetToken = null;
    }

    public void requestEmailChange(String newEmail, String token) {
        this.pendingEmail = newEmail;
        this.emailChangeToken = token;
    }

    public void confirmEmailChange(String token) {
        if (this.emailChangeToken == null || !this.emailChangeToken.equals(token)) {
            throw new IllegalArgumentException("Invalid or expired email change link");
        }
        this.email = this.pendingEmail;
        this.pendingEmail = null;
        this.emailChangeToken = null;
    }

    public void updateProfile(String firstName, String lastName, String phoneNumber) {
        if (firstName != null && !firstName.isBlank()) {
            this.firstName = firstName;
        }
        if (lastName != null && !lastName.isBlank()) {
            this.lastName = lastName;
        }
        if (phoneNumber != null) {
            this.phoneNumber = phoneNumber;
        }
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public void updateDisabledNotificationCategories(String disabledCategoriesCsv) {
        this.disabledNotificationCategories = disabledCategoriesCsv;
    }

    public boolean isNotificationCategoryEnabled(String category) {
        if (disabledNotificationCategories == null || disabledNotificationCategories.isBlank()) {
            return true;
        }
        for (String disabled : disabledNotificationCategories.split(",")) {
            if (disabled.trim().equalsIgnoreCase(category)) {
                return false;
            }
        }
        return true;
    }
}
