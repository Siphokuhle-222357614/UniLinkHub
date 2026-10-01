package za.co.unilinkhub.media.domain;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * An uploaded business logo or listing photo. Stored in the database rather than on disk because
 * hosts like Render wipe the local disk on every redeploy, and the database is already backed up.
 * Images are resized in the browser before upload, so rows stay small (typically 100-400 KB).
 */
@Entity
@Table(name = "images")
@Getter
@NoArgsConstructor
public class StoredImage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "uploader_id", nullable = false)
    private UUID uploaderId;

    @Column(name = "content_type", nullable = false, length = 30)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private int sizeBytes;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(nullable = false, length = 6_000_000)
    private byte[] data;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static StoredImage of(UUID uploaderId, String contentType, byte[] data) {
        StoredImage image = new StoredImage();
        image.uploaderId = uploaderId;
        image.contentType = contentType;
        image.sizeBytes = data.length;
        image.data = data;
        return image;
    }
}
