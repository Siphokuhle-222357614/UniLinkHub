package za.co.unilinkhub.saved.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.unilinkhub.common.exception.ResourceNotFoundException;
import za.co.unilinkhub.listing.application.ListingDTO;
import za.co.unilinkhub.listing.repository.ListingRepository;
import za.co.unilinkhub.saved.domain.SavedListing;
import za.co.unilinkhub.saved.repository.SavedListingRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SavedListingService {

    private final SavedListingRepository savedListingRepository;
    private final ListingRepository listingRepository;

    public void save(UUID userId, UUID listingId) {
        if (listingRepository.findById(listingId).isEmpty()) {
            throw new ResourceNotFoundException("Listing not found");
        }
        if (!savedListingRepository.existsByUserIdAndListingId(userId, listingId)) {
            savedListingRepository.save(SavedListing.create(userId, listingId));
        }
    }

    @Transactional
    public void unsave(UUID userId, UUID listingId) {
        savedListingRepository.deleteByUserIdAndListingId(userId, listingId);
    }

    public long countSaves(UUID listingId) {
        return savedListingRepository.countByListingId(listingId);
    }

    public List<ListingDTO> mine(UUID userId) {
        return savedListingRepository.findByUserId(userId).stream()
                .sorted(Comparator.comparing(SavedListing::getCreatedAt).reversed())
                .map(SavedListing::getListingId)
                .map(listingRepository::findById)
                .flatMap(Optional::stream)
                .map(ListingDTO::from)
                .toList();
    }
}
