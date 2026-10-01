package za.co.unilinkhub.listing.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import za.co.unilinkhub.business.domain.Business;
import za.co.unilinkhub.business.repository.BusinessRepository;
import za.co.unilinkhub.trust.application.SellerTrust;
import za.co.unilinkhub.trust.application.SellerTrustService;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Adds "who's selling this and can I trust them" to listings in bulk: business name, campus,
 * pickup spot and trust signals - one lookup per business, not per listing.
 */
@Component
@RequiredArgsConstructor
public class ListingEnricher {

    private final BusinessRepository businessRepository;
    private final SellerTrustService sellerTrustService;

    public List<ListingDTO> enrich(List<ListingDTO> listings) {
        if (listings.isEmpty()) {
            return listings;
        }
        Set<UUID> ids = listings.stream().map(ListingDTO::businessId).collect(Collectors.toSet());
        Map<UUID, Business> businesses = ids.stream().map(businessRepository::findById).flatMap(Optional::stream)
                .collect(Collectors.toMap(Business::getId, Function.identity()));
        Map<UUID, SellerTrust> trust = sellerTrustService.forBusinesses(ids);
        return listings.stream().map(l -> {
            Business b = businesses.get(l.businessId());
            if (b == null) {
                return l;
            }
            return l.withSeller(b.getBusinessName(),
                    b.getCampus() == null ? null : b.getCampus().name(),
                    b.getCampus() == null ? null : b.getCampus().label(),
                    b.getPickupLocation(), trust.get(b.getId()));
        }).toList();
    }

    public ListingDTO enrich(ListingDTO listing) {
        return enrich(List.of(listing)).get(0);
    }
}
