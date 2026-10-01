package za.co.unilinkhub.user.domain;

import jakarta.persistence.Column;
import za.co.unilinkhub.shared.domain.Campus;
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

    /** Null for admin accounts - admins are staff, created by invitation, not CPUT students. */
    @Column(name = "student_number", length = 32)
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

    @Column(name = "verification_token_expires_at")
    private LocalDateTime verificationTokenExpiresAt;

    @Column(name = "password_reset_token", length = 64)
    private String passwordResetToken;

    @Column(name = "password_reset_token_expires_at")
    private LocalDateTime passwordResetTokenExpiresAt;

    @Column(name = "pending_email", length = 254)
    private String pendingEmail;

    @Column(name = "email_change_token", length = 64)
    private String emailChangeToken;

    @Column(name = "suspension_reason", length = 1000)
    private String suspensionReason;

    /** When this student agreed to the marketplace rules (no alcohol, drugs, weapons...) before selling. */
    @Column(name = "seller_rules_accepted_at")
    private LocalDateTime sellerRulesAcceptedAt;

    /** The student's home campus, used to suggest listings they can actually collect. Optional. */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Campus campus;

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

    private User(String studentNumber, String firstName, String lastName, String email, String passwordHash) {
        this.studentNumber = studentNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    /** A student signing themselves up - inactive until they click the link sent to their email. */
    public static User register(String studentNumber, String firstName, String lastName, String email,
                                 String passwordHash, String verificationToken, LocalDateTime tokenExpiresAt) {
        User user = new User(studentNumber, firstName, lastName, email, passwordHash);
        user.verificationToken = verificationToken;
        user.verificationTokenExpiresAt = tokenExpiresAt;
        return user;
    }

    /**
     * An admin account, created by another admin (or the startup bootstrap). It's a separate account
     * from any student account: no student number, never a seller. Its email is proven by the
     * invite link, so it starts ACTIVE with an unusable password until the invitee sets one.
     */
    public static User createAdmin(String firstName, String lastName, String email, String passwordHash) {
        User user = new User(null, firstName, lastName, email, passwordHash);
        user.role = UserRole.ADMIN;
        user.accountStatus = AccountStatus.ACTIVE;
        return user;
    }

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    public void verifyEmail(String token, LocalDateTime now) {
        if (this.accountStatus != AccountStatus.PENDING_VERIFICATION) {
            throw new IllegalStateException("This email address is already verified, so you can log in.");
        }
        if (this.verificationToken == null || !this.verificationToken.equals(token)) {
            throw new IllegalArgumentException("This verification link isn't valid. Please use the latest link we emailed you, or request a new one.");
        }
        if (verificationTokenExpiresAt != null && now.isAfter(verificationTokenExpiresAt)) {
            throw new IllegalArgumentException("This verification link has expired. Request a new one from the login page - it only takes a second.");
        }
        this.accountStatus = AccountStatus.ACTIVE;
        this.verificationToken = null;
        this.verificationTokenExpiresAt = null;
    }

    /** Issues a fresh verification link (the old one stops working). */
    public void renewVerificationToken(String token, LocalDateTime expiresAt) {
        if (this.accountStatus != AccountStatus.PENDING_VERIFICATION) {
            throw new IllegalStateException("This email address is already verified, so you can log in.");
        }
        this.verificationToken = token;
        this.verificationTokenExpiresAt = expiresAt;
    }

    public void becomeSeller(LocalDateTime acceptedRulesAt) {
        if (isAdmin()) {
            throw new IllegalStateException("Admin accounts can't become sellers.");
        }
        this.seller = true;
        if (this.sellerRulesAcceptedAt == null) {
            this.sellerRulesAcceptedAt = acceptedRulesAt;
        }
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

    public void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
    }

    public void requestPasswordReset(String token, LocalDateTime expiresAt) {
        this.passwordResetToken = token;
        this.passwordResetTokenExpiresAt = expiresAt;
    }

    public void resetPassword(String token, String newPasswordHash, LocalDateTime now) {
        if (this.passwordResetToken == null || !this.passwordResetToken.equals(token)) {
            throw new IllegalArgumentException("This reset link isn't valid any more - it may have been used already. Please request a new one.");
        }
        if (passwordResetTokenExpiresAt != null && now.isAfter(passwordResetTokenExpiresAt)) {
            throw new IllegalArgumentException("This reset link has expired. Please request a new one - it only takes a second.");
        }
        this.passwordHash = newPasswordHash;
        this.passwordResetToken = null;
        this.passwordResetTokenExpiresAt = null;
    }

    public void requestEmailChange(String newEmail, String token) {
        this.pendingEmail = newEmail;
        this.emailChangeToken = token;
    }

    public void confirmEmailChange(String token) {
        if (this.emailChangeToken == null || !this.emailChangeToken.equals(token)) {
            throw new IllegalArgumentException("This email change link isn't valid any more - it may have been used already. Request a new one from your account settings.");
        }
        this.email = this.pendingEmail;
        this.pendingEmail = null;
        this.emailChangeToken = null;
    }

    /** {@code clearCampus} distinguishes "remove my campus" from "campus not sent". */
    public void updateCampus(Campus campus, boolean clearCampus) {
        if (clearCampus) {
            this.campus = null;
        } else if (campus != null) {
            this.campus = campus;
        }
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
