package za.co.unilinkhub.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * An in-app notification for one user. There is no email delivery in this build, so this
 * (plus the bell icon) is the only way a user finds out about verification decisions, booking
 * updates, stock/price alerts and new reviews.
 */
@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 30)
    private String category;

    @Column(nullable = false, length = 500)
    private String message;

    // Named explicitly: "read" is a reserved word in MySQL, and an unquoted CREATE TABLE column
    // named `read` fails at the database - Hibernate's ddl-auto=update logs that as a warning
    // and moves on, so the whole table silently never gets created without this.
    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private Notification(UUID userId, String category, String message) {
        this.userId = userId;
        this.category = category;
        this.message = message;
    }

    public static Notification create(UUID userId, String category, String message) {
        return new Notification(userId, category, message);
    }

    public void markRead() {
        this.read = true;
    }
}
