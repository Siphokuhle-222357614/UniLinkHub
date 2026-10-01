package za.co.unilinkhub.media.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.unilinkhub.media.domain.StoredImage;
import za.co.unilinkhub.media.repository.ImageRepository;

import java.util.UUID;

public interface JpaImageRepository extends JpaRepository<StoredImage, UUID>, ImageRepository {
}
