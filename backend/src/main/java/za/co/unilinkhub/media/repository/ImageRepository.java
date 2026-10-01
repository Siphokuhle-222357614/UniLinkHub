package za.co.unilinkhub.media.repository;

import za.co.unilinkhub.media.domain.StoredImage;

import java.util.Optional;
import java.util.UUID;

public interface ImageRepository {

    StoredImage save(StoredImage image);

    Optional<StoredImage> findById(UUID id);
}
