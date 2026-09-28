package za.co.unilinkhub.announcement.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.unilinkhub.announcement.domain.Announcement;

import java.util.UUID;

public interface JpaAnnouncementRepository extends JpaRepository<Announcement, UUID>, za.co.unilinkhub.announcement.repository.AnnouncementRepository {
}
