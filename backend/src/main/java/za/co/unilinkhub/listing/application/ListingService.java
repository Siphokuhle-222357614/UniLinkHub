package za.co.unilinkhub.listing.application;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.unilinkhub.business.domain.Business;
import za.co.unilinkhub.business.repository.BusinessRepository;
import za.co.unilinkhub.common.exception.BadRequestException;
import za.co.unilinkhub.common.exception.ForbiddenException;
import za.co.unilinkhub.common.exception.ResourceNotFoundException;
import za.co.unilinkhub.common.web.PageResponse;
import za.co.unilinkhub.listing.domain.Listing;
import za.co.unilinkhub.listing.domain.ListingStatus;
import za.co.unilinkhub.listing.domain.Product;
import za.co.unilinkhub.listing.repository.CategoryCount;
import za.co.unilinkhub.listing.repository.ListingRepository;
import za.co.unilinkhub.media.application.ImageService;
import za.co.unilinkhub.moderation.RestrictedItemsPolicy;
import za.co.unilinkhub.notification.application.NotificationService;
import za.co.unilinkhub.savedsearch.application.SavedSearchService;
import za.co.unilinkhub.security.CurrentUserProvider;
import za.co.unilinkhub.shared.domain.Campus;
import za.co.unilinkhub.stockalert.application.StockAlertService;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ListingService {

    static final String REMOVED_LISTING_MESSAGE =
            "This listing has been removed because it broke UniLinkHub's marketplace rules.";

    private final ListingRepository listingRepository;
    private final BusinessRepository businessRepository;
    private final StockAlertService stockAlertService;
    private final NotificationService notificationService;
    private final SavedSearchService savedSearchService;
    private final RestrictedItemsPolicy restrictedItemsPolicy;
    private final ListingEnricher listingEnricher;

    public ListingDTO createProduct(UUID requesterId, UUID businessId, String name, String description,
                                     String category, BigDecimal price, Integer stockQuantity, String imageUrl,
                                     List<String> imageUrls) {
        assertOwnership(businessId, requesterId);
        restrictedItemsPolicy.requireAllowed("listing", name, description);
        List<String> photos = resolvePhotos(imageUrls, imageUrl, List.of());
        requireNonNegativeStock(stockQuantity);
        Product product = Product.create(businessId, name.trim(), description.trim(), category, price, stockQuantity, null);
        if (photos != null) {
            product.updatePhotos(photos);
        }
        Listing saved = listingRepository.save(product);
        savedSearchService.notifyMatchingSearches(saved);
        return ListingDTO.from(saved);
    }

    public ListingDTO createService(UUID requesterId, UUID businessId, String name, String description,
                                     String category, BigDecimal price, Integer durationMinutes,
                                     String availabilitySchedule, String imageUrl, List<String> imageUrls) {
        assertOwnership(businessId, requesterId);
        restrictedItemsPolicy.requireAllowed("listing", name, description, availabilitySchedule);
        List<String> photos = resolvePhotos(imageUrls, imageUrl, List.of());
        za.co.unilinkhub.listing.domain.Service service = za.co.unilinkhub.listing.domain.Service.create(
                businessId, name.trim(), description.trim(), category, price, durationMinutes, availabilitySchedule, null);
        if (photos != null) {
            service.updatePhotos(photos);
        }
        Listing saved = listingRepository.save(service);
        savedSearchService.notifyMatchingSearches(saved);
        return ListingDTO.from(saved);
    }

    public ListingDTO update(UUID requesterId, UUID listingId, String name, String description, String category,
                              BigDecimal price, Integer stockQuantity, String imageUrl, List<String> imageUrls,
                              Integer lowStockThreshold, Integer durationMinutes, String availabilitySchedule, String status) {
        Listing listing = findListing(listingId);
        assertOwnership(listing.getBusinessId(), requesterId);
        restrictedItemsPolicy.requireAllowed("listing",
                name != null ? name : listing.getName(),
                description != null ? description : listing.getDescription(),
                availabilitySchedule);
        List<String> photos = resolvePhotos(imageUrls, imageUrl, listing.getGallery());
        requireNonNegativeStock(stockQuantity);
        listing.updateBasicDetails(name, description, category, price);

        // Explicit active/inactive requests apply first; for a Product, the stock update that
        // follows has the final say (0 in stock always means SOLD_OUT, regardless of what was
        // requested here) since "in stock" is an objective fact rather than a manual toggle.
        if ("ACTIVE".equalsIgnoreCase(status)) {
            if (listing.getStatus() == ListingStatus.INACTIVE) {
                requireNotTakenDown(listing);
            }
            listing.reactivate();
        } else if ("INACTIVE".equalsIgnoreCase(status)) {
            listing.deactivate();
        }
        if (photos != null) {
            listing.updatePhotos(photos);
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
     * Reduces a product's stock as part of an order being placed (see PlaceOrderUseCase). Runs
     * inside that use case's transaction with the listing's row locked, so concurrent checkouts
     * can't both take the last item.
     */
    public void decrementStock(UUID listingId, int quantity) {
        Listing listing = listingRepository.findByIdForUpdate(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("We couldn't find that listing. It may have been removed by the seller."));
        if (!(listing instanceof Product product)) {
            throw new BadRequestException("Only products can be bought through checkout. Services are booked from their listing page instead.");
        }
        int current = product.getStockQuantity() == null ? 0 : product.getStockQuantity();
        if (current < quantity) {
            throw new BadRequestException(current == 0
                    ? "Sorry, \"" + product.getName() + "\" has just sold out. Please remove it from your cart."
                    : "Sorry, there " + (current == 1 ? "is only 1" : "are only " + current) + " of \"" + product.getName()
                    + "\" left. Lower the quantity in your cart and try again.");
        }
        boolean wasLowStock = product.isLowStock();
        product.updateStock(current - quantity);
        if (!wasLowStock && product.isLowStock()) {
            notifyLowStock(product);
        }
        listingRepository.save(product);
    }

    /** Puts stock back when an order is cancelled, so a cancellation never loses inventory. */
    public void restoreStock(UUID listingId, int quantity) {
        listingRepository.findByIdForUpdate(listingId).ifPresent(listing -> {
            if (listing instanceof Product product) {
                boolean wasSoldOut = product.getStatus() == ListingStatus.SOLD_OUT;
                int current = product.getStockQuantity() == null ? 0 : product.getStockQuantity();
                product.updateStock(current + quantity);
                listingRepository.save(product);
                if (wasSoldOut && product.getStatus() == ListingStatus.ACTIVE) {
                    stockAlertService.notifyAndClear(product.getId(), product.getName());
                }
            }
        });
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
        requireNotTakenDown(listing);
        listing.reactivate();
        listingRepository.save(listing);
    }

    /**
     * Public listing page. A listing an admin has taken down is hidden from everyone except its
     * owner (who needs to see why) and admins.
     */
    @Transactional
    public ListingDTO getById(UUID id) {
        Listing listing = findListing(id);
        if (listing.isTakenDown() && !canSeeRemoved(listing)) {
            throw new ResourceNotFoundException(REMOVED_LISTING_MESSAGE);
        }
        listing.recordView();
        return listingEnricher.enrich(ListingDTO.from(listingRepository.save(listing)));
    }

    public static final int MAX_PAGE_SIZE = 48;

    /** Browse: one page of live listings, filtered and sorted by the database. */
    public PageResponse<ListingDTO> searchPage(String category, String keyword, BigDecimal minPrice, BigDecimal maxPrice,
                                               String type, boolean verifiedOnly, String campus, String sort,
                                               int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), MAX_PAGE_SIZE), sortFor(sort));
        Page<Listing> result = listingRepository.searchPage(blankToNull(category), blankToNull(keyword), minPrice, maxPrice,
                normaliseKind(type), verifiedOnly, Campus.parseOptional(campus), pageable);
        return PageResponse.of(result, listings -> listingEnricher.enrich(listings.stream().map(ListingDTO::from).toList()));
    }

    /** The older list endpoint - now just the first page, capped, for small widgets (trending, suggestions). */
    public List<ListingDTO> search(String category, String keyword, BigDecimal minPrice, BigDecimal maxPrice,
                                    String type, boolean verifiedOnly, String campus, String sort, int limit) {
        return searchPage(category, keyword, minPrice, maxPrice, type, verifiedOnly, campus, sort, 0, limit).items();
    }

    public Map<String, Long> categoryCounts() {
        return listingRepository.countActiveByCategory().stream()
                .collect(Collectors.toMap(CategoryCount::category, CategoryCount::count, Long::sum, TreeMap::new));
    }

    private static Sort sortFor(String sort) {
        Sort primary = switch (sort == null ? "" : sort) {
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "price");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "price");
            case "views" -> Sort.by(Sort.Direction.DESC, "viewCount");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
        // A unique tie-breaker keeps pages stable: no listing appears twice or goes missing between pages.
        return primary.and(Sort.by(Sort.Direction.ASC, "id"));
    }

    private static String normaliseKind(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        String upper = type.trim().toUpperCase();
        if (!upper.equals("PRODUCT") && !upper.equals("SERVICE")) {
            throw new BadRequestException("Listing type must be \"Product\" or \"Service\".");
        }
        return upper;
    }

    /**
     * Works out a listing's new photo list from either the gallery field or the older single-photo
     * field. Null means "photos weren't sent - leave them as they are". New photos must have been
     * uploaded to UniLinkHub; ones the listing already has are always accepted.
     */
    private static List<String> resolvePhotos(List<String> imageUrls, String imageUrl, List<String> current) {
        List<String> wanted;
        if (imageUrls != null) {
            wanted = imageUrls.stream().filter(u -> u != null && !u.isBlank()).map(String::trim).distinct().toList();
        } else if (imageUrl != null) {
            wanted = imageUrl.isBlank() ? List.of() : List.of(imageUrl.trim());
        } else {
            return null;
        }
        if (wanted.size() > Listing.MAX_PHOTOS) {
            throw new BadRequestException("You can add up to " + Listing.MAX_PHOTOS + " photos to a listing - you've chosen "
                    + wanted.size() + ". Remove a few and try again.");
        }
        for (String url : wanted) {
            ImageService.requireUploadedImageUrl(url, current.contains(url) ? url : null);
        }
        return wanted;
    }

    public List<ListingDTO> byBusiness(UUID businessId) {
        boolean ownerOrAdmin = CurrentUserProvider.isAdmin() || isOwner(businessId);
        return listingEnricher.enrich(listingRepository.findByBusinessId(businessId).stream()
                .filter(l -> ownerOrAdmin || !l.isTakenDown())
                .map(ListingDTO::from)
                .toList());
    }

    public List<ListingDTO> listMine(UUID ownerId) {
        return businessRepository.findByOwnerId(ownerId).stream()
                .flatMap(b -> listingRepository.findByBusinessId(b.getId()).stream())
                .sorted(Comparator.comparing(Listing::getCreatedAt).reversed())
                .map(ListingDTO::from)
                .toList();
    }

    private boolean canSeeRemoved(Listing listing) {
        return CurrentUserProvider.isAdmin() || isOwner(listing.getBusinessId());
    }

    private boolean isOwner(UUID businessId) {
        Optional<UUID> me = CurrentUserProvider.currentId();
        return me.isPresent() && businessRepository.findById(businessId)
                .map(b -> b.getOwnerId().equals(me.get())).orElse(false);
    }

    private static void requireNotTakenDown(Listing listing) {
        if (listing.isTakenDown()) {
            throw new ForbiddenException("This listing was removed by an admin because it broke the marketplace rules"
                    + (listing.getTakedownReason() != null ? " (" + listing.getTakedownReason() + ")" : "")
                    + ", so it can't be shown again. If you think this was a mistake, please contact an admin.");
        }
    }

    private static void requireNonNegativeStock(Integer stockQuantity) {
        if (stockQuantity != null && stockQuantity < 0) {
            throw new BadRequestException("Stock can't be less than zero.");
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private Listing findListing(UUID id) {
        return listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("We couldn't find that listing. It may have been removed by the seller."));
    }

    private void assertOwnership(UUID businessId, UUID requesterId) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("We couldn't find that business. It may have been removed."));
        if (!business.getOwnerId().equals(requesterId)) {
            throw new ForbiddenException("Only the owner of this business can do this. You can only manage businesses you created yourself.");
        }
    }
}
