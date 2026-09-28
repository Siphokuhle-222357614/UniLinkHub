package za.co.unilinkhub.savedsearch.repository;

import za.co.unilinkhub.savedsearch.domain.SavedSearch;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SavedSearchRepository {

    SavedSearch save(SavedSearch savedSearch);

    Optional<SavedSearch> findById(UUID id);

    List<SavedSearch> findByUserId(UUID userId);

    List<SavedSearch> findAll();

    void deleteById(UUID id);
}
