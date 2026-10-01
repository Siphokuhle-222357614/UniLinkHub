package za.co.unilinkhub.savedsearch.application;

import za.co.unilinkhub.common.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import za.co.unilinkhub.common.exception.ResourceNotFoundException;
import za.co.unilinkhub.listing.domain.Listing;
import za.co.unilinkhub.listing.repository.ListingRepository;
import za.co.unilinkhub.notification.application.NotificationService;
import za.co.unilinkhub.savedsearch.domain.SavedSearch;
import za.co.unilinkhub.savedsearch.repository.SavedSearchRepository;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SavedSearchService {

    private final SavedSearchRepository savedSearchRepository;
    private final ListingRepository listingRepository;
    private final NotificationService notificationService;

    public SavedSearchDTO save(UUID userId, String label, String keyword, String category, BigDecimal maxPrice, String listingType) {
        SavedSearch savedSearch = SavedSearch.create(userId, label, keyword, category, maxPrice, listingType);
        return toDTO(savedSearchRepository.save(savedSearch));
    }

    public List<SavedSearchDTO> listMine(UUID userId) {
        return savedSearchRepository.findByUserId(userId).stream()
                .sorted(Comparator.comparing(SavedSearch::getCreatedAt).reversed())
                .map(this::toDTO)
                .toList();
    }

    public SavedSearchDTO setAlertsEnabled(UUID userId, UUID id, boolean enabled) {
        SavedSearch savedSearch = findOwned(id, userId);
        savedSearch.setAlertsEnabled(enabled);
        return toDTO(savedSearchRepository.save(savedSearch));
    }

    public SavedSearchDTO markViewed(UUID userId, UUID id) {
        SavedSearch savedSearch = findOwned(id, userId);
        savedSearch.markViewed();
        return toDTO(savedSearchRepository.save(savedSearch));
    }

    public void delete(UUID userId, UUID id) {
        findOwned(id, userId);
        savedSearchRepository.deleteById(id);
    }

    /**
     * Called whenever a new listing is published (see ListingService) so every saved search
     * with alerts on gets checked against just that one listing, rather than everyone having
     * to re-run their search to find out something new matched.
     */
    public void notifyMatchingSearches(Listing listing) {
        for (SavedSearch savedSearch : savedSearchRepository.findAll()) {
            if (savedSearch.isAlertsEnabled() && savedSearch.matches(listing)) {
                notificationService.notify(savedSearch.getUserId(), "SAVED_SEARCH",
                        "New listing matches your saved search \"" + savedSearch.getLabel() + "\": " + listing.getName());
            }
        }
    }

    private long countNewMatches(SavedSearch savedSearch) {
        List<Listing> candidates = listingRepository.search(savedSearch.getCategory(), savedSearch.getKeyword(), null, savedSearch.getMaxPrice());
        return candidates.stream()
                .filter(l -> savedSearch.getListingType() == null || savedSearch.getListingType().isBlank()
                        || l.getClass().getSimpleName().equalsIgnoreCase(savedSearch.getListingType()))
                .filter(l -> l.getCreatedAt().isAfter(savedSearch.getLastViewedAt()))
                .count();
    }

    private SavedSearch findOwned(UUID id, UUID userId) {
        SavedSearch savedSearch = savedSearchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("We couldn't find that saved search."));
        if (!savedSearch.getUserId().equals(userId)) {
            throw new ForbiddenException("You can only change your own saved searches.");
        }
        return savedSearch;
    }

    private SavedSearchDTO toDTO(SavedSearch savedSearch) {
        return SavedSearchDTO.from(savedSearch, countNewMatches(savedSearch));
    }
}
