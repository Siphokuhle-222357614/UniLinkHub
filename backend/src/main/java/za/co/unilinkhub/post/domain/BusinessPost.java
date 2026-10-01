package za.co.unilinkhub.post.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * An update a business shares on its provider page - like a post on a Facebook page. It can carry
 * a photo and point at one of the business's own listings, so a post is always one tap away from
 * something a student can actually buy or book.
 */
@Entity
@Table(name = "business_posts", indexes = @Index(name = "idx_business_posts_business", columnList = "business_id"))
@Getter
@NoArgsConstructor
public class BusinessPost {

    public static final int MAX_LENGTH = 2000;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Column(length = MAX_LENGTH)
    private String body;

    @Column(name = "image_url")
    private String imageUrl;

    /** Optional: one of this business's listings, shown as a tappable card under the post. */
    @Column(name = "listing_id")
    private UUID listingId;

    @Column(nullable = false)
    private boolean pinned = false;

    @Column(nullable = false)
    private boolean edited = false;

    @Column(name = "flag_count", nullable = false)
    private int flagCount = 0;

    /** Set when an admin removes the post for breaking the rules; it's then hidden from everyone but its owner and admins. */
    @Column(name = "removed_reason", length = 1000)
    private String removedReason;

    @Column(name = "removed_at")
    private LocalDateTime removedAt;

    // Set in the factory rather than by @CreationTimestamp: that only fills the field when the row is
    // flushed, after the API response has been built, so a just-created one went out with no date.
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static BusinessPost create(UUID businessId, UUID authorId, String body, String imageUrl, UUID listingId) {
        BusinessPost post = new BusinessPost();
        post.businessId = businessId;
        post.authorId = authorId;
        post.body = body;
        post.imageUrl = imageUrl;
        post.listingId = listingId;
        post.createdAt = LocalDateTime.now();
        return post;
    }

    public void edit(String body, String imageUrl, UUID listingId) {
        this.body = body;
        this.imageUrl = imageUrl;
        this.listingId = listingId;
        this.edited = true;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    public void flag() {
        this.flagCount++;
    }

    public boolean isRemoved() {
        return removedAt != null;
    }

    public void remove(String reason, LocalDateTime when) {
        this.removedReason = reason;
        this.removedAt = when;
        this.pinned = false;
    }
}
