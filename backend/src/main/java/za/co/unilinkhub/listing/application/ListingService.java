package za.co.unilinkhub.listing.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.unilinkhub.business.domain.Business;
import za.co.unilinkhub.business.domain.VerificationStatus;
import za.co.unilinkhub.business.repository.BusinessRepository;
import za.co.unilinkhub.common.exception.BadRequestException;
import za.co.unilinkhub.common.exception.ResourceNotFoundException;
import za.co.unilinkhub.common.exception.UnauthorizedException;
import za.co.unilinkhub.listing.domain.Listing;
import za.co.unilinkhub.listing.domain.ListingStatus;
import za.co.unilinkhub.listing.domain.Product;
import za.co.unilinkhub.listing.repository.ListingRepository;
import za.co.unilinkhub.notification.application.NotificationService;
import za.co.unilinkhub.savedsearch.application.SavedSearchService;
import za.co.unilinkhub.stockalert.application.StockAlertService;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ListingService {

    private final ListingRepository listingRepository;
    private final BusinessRepository businessRepository;
    private final StockAlertService stockAlertService;
    private final NotificationService notificationService;
    private final SavedSearchService savedSearchService;

    public ListingDTO createProduct(UUID requesterId, UUID businessId, String name, String description,
                                     String category, BigDecimal price, Integer stockQuantity, String imageUrl) {
        assertOwnership(businessId, requesterId);
        Product product = Product.create(businessId, name, description, category, price, stockQuantity, imageUrl);
        Listing saved = listingRepository.save(product);
        savedSearchService.notifyMatchingSearches(saved);
        return ListingDTO.from(saved);
    }

    public ListingDTO createService(UUID requesterId, UUID businessId, String name, String description,
                                     String category, BigDecimal price, Integer durationMinutes,
                                     String availabilitySchedule) {
        assertOwnership(businessId, requesterId);
        za.co.unilinkhub.listing.domain.Service service = za.co.unilinkhub.listing.domain.Service.create(
                businessId, name, description, category, price, durationMinutes, availabilitySchedule);
        Listing saved = listingRepository.save(service);
        savedSearchService.notifyMatchingSearches(saved);
        return ListingDTO.from(saved);
    }

    public ListingDTO update(UUID requesterId, UUID listingId, String name, String description, String category,
                              BigDecimal price, Integer stockQuantity, String imageUrl, Integer lowStockThreshold,
                              Integer durationMinutes, String availabilitySchedule, String status) {
        Listing listing = findListing(listingId);
        assertOwnership(listing.getBusinessId(), requesterId);
        listing.updateBasicDetails(name, description, category, price);

        // Explicit active/inactive requests apply first; for a Product, the stock update that
        // follows has the final say (0 in stock always means SOLD_OUT, regardless of what was
        // requested here) since "in stock" is an objective fact rather than a manual toggle.
        if ("ACTIVE".equalsIgnoreCase(status)) {
            listing.reactivate();
        } else if ("INACTIVE".equalsIgnoreCase(status)) {
            listing.deactivate();
        }

        if (listing instanceof Product product) {
            boolean wasSoldOut = product.getStatus() == ListingStatus.SOLD_OUT;
            boolean wasLowStock = product.isLowStock();
            if (lowStockThreshold != null) {
                product.updateLowStockThreshold(lowStockThreshold);
            }
            if (stockQuantity != null) {
                product.updateStock(stockQuantity);
            }
            if (imageUrl != null) {
                product.updateImageUrl(imageUrl.isBlank() ? null : imageUrl);
            }

            if (wasSoldOut && product.getStatus() == ListingStatus.ACTIVE) {
                stockAlertService.notifyAndClear(product.getId(), product.getName());
            } else if (!wasLowStock && product.isLowStock()) {
                notifyLowStock(product);
            }
        }
        if (listing instanceof za.co.unilinkhub.listing.domain.Service service) {
            if (durationMinutes != null) {
                service.updateDuration(durationMinutes);
            }
            if (availabilitySchedule != null && !availabilitySchedule.isBlank()) {
                service.updateSchedule(availabilitySchedule);
            }
        }

        return ListingDTO.from(listingRepository.save(listing));
    }

    /**
     * Reduces a product's stock by the given quantity as part of an order being placed
     * (see PlaceOrderUseCase), reusing the same low-stock notification trigger as a manual
     * stock edit so a low-stock alert fires however the stock actually changed.
     */
    public void decrementStock(UUID listingId, int quantity) {
        Listing listing = findListing(listingId);
        if (!(listing instanceof Product product)) {
            throw new BadRequestException("Only product listings can be purchased through checkout");
        }
        int current = product.getStockQuantity() == null ? 0 : product.getStockQuantity();
        if (current < quantity) {
            throw new BadRequestException("Not enough stock for \"" + product.getName() + "\"");
        }
        boolean wasLowStock = product.isLowStock();
        product.updateStock(current - quantity);
        if (!wasLowStock && product.isLowStock()) {
            notifyLowStock(product);
        }
        listingRepository.save(product);
    }

    private void notifyLowStock(Product product) {
        businessRepository.findById(product.getBusinessId()).ifPresent(business ->
                notificationService.notify(business.getOwnerId(), "STOCK",
                        "\"" + product.getName() + "\" is low on stock - " + product.getStockQuantity() + " left"));
    }

    public void deactivate(UUID requesterId, UUID listingId) {
        Listing listing = findListing(listingId);
        assertOwnership(listing.getBusinessId(), requesterId);
        listing.deactivate();
        listingRepository.save(listing);
    }

    public void reactivate(UUID requesterId, UUID listingId) {
        Listing listing = findListing(listingId);
        assertOwnership(listing.getBusinessId(), requesterId);
        listing.reactivate();
        listingRepository.save(listing);
    }

    @Transactional
    public ListingDTO getById(UUID id) {
        Listing listing = findListing(id);
        listing.recordView();
        return ListingDTO.from(listingRepository.save(listing));
    }

    public List<ListingDTO> search(String category, String keyword, BigDecimal minPrice, BigDecimal maxPrice,
                                    String type, boolean verifiedOnly, String sort) {
        List<Listing> listings = listingRepository.search(category, keyword, minPrice, maxPrice);

        if (type != null && !type.isBlank()) {
            listings = listings.stream().filter(l -> matchesType(l, type)).toList();
        }

        if (verifiedOnly) {
            Set<UUID> verifiedBusinessIds = businessRepository.findByVerificationStatus(VerificationStatus.VERIFIED)
                    .stream().map(Business::getId).collect(Collectors.toSet());
            listings = listings.stream().filter(l -> verifiedBusinessIds.contains(l.getBusinessId())).toList();
        }

        Comparator<Listing> comparator = switch (sort == null ? "" : sort) {
            case "price_asc" -> Comparator.comparing(Listing::getPrice);
            case "price_desc" -> Comparator.comparing(Listing::getPrice).reversed();
            case "views" -> Comparator.comparingLong(Listing::getViewCount).reversed();
            default -> Comparator.comparing(Listing::getCreatedAt).reversed();
        };

        return listings.stream().sorted(comparator).map(ListingDTO::from).toList();
    }

    private boolean matchesType(Listing listing, String type) {
        return listing.getClass().getSimpleName().equalsIgnoreCase(type);
    }

    public List<ListingDTO> byBusiness(UUID businessId) {
        return listingRepository.findByBusinessId(businessId).stream().map(ListingDTO::from).toList();
    }

    public List<ListingDTO> listMine(UUID ownerId) {
        return businessRepository.findByOwnerId(ownerId).stream()
                .flatMap(b -> listingRepository.findByBusinessId(b.getId()).stream())
                .sorted(Comparator.comparing(Listing::getCreatedAt).reversed())
                .map(ListingDTO::from)
                .toList();
    }

    private Listing findListing(UUID id) {
        return listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));
    }

    private void assertOwnership(UUID businessId, UUID requesterId) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));
        if (!business.getOwnerId().equals(requesterId)) {
            throw new UnauthorizedException("You do not own this business");
        }
    }
}
