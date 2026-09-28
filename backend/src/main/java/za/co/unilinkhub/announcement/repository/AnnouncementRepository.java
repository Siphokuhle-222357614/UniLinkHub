package za.co.unilinkhub.announcement.repository;

import za.co.unilinkhub.announcement.domain.Announcement;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AnnouncementRepository {
    Announcement save(Announcement announcement);
    Optional<Announcement> findById(UUID id);
    List<Announcement> findAll();
    List<Announcement> findByActiveTrue();
}
