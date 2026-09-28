package za.co.unilinkhub.announcement.domain;

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
 * A site-wide banner message an admin can publish (e.g. planned maintenance). Only one is
 * meant to be active at a time - publishing a new active one deactivates any other.
 */
@Entity
@Table(name = "announcements")
@Getter
@NoArgsConstructor
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(nullable = false)
    private boolean active;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private Announcement(String message, boolean active) {
        this.message = message;
        this.active = active;
    }

    public static Announcement publish(String message, boolean active) {
        return new Announcement(message, active);
    }

    public void deactivate() {
        this.active = false;
    }
}
