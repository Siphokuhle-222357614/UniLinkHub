package za.co.unilinkhub.savedsearch.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.unilinkhub.savedsearch.domain.SavedSearch;
import za.co.unilinkhub.savedsearch.repository.SavedSearchRepository;

import java.util.List;
import java.util.UUID;

public interface JpaSavedSearchRepository extends JpaRepository<SavedSearch, UUID>, SavedSearchRepository {
    @Override
    List<SavedSearch> findByUserId(UUID userId);
}
