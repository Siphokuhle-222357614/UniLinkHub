package za.co.unilinkhub.business.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import za.co.unilinkhub.audit.application.AuditLogService;
import za.co.unilinkhub.business.domain.Business;
import za.co.unilinkhub.business.domain.VerificationStatus;
import za.co.unilinkhub.business.repository.BusinessRepository;
import za.co.unilinkhub.common.exception.ResourceNotFoundException;
import za.co.unilinkhub.common.exception.UnauthorizedException;
import za.co.unilinkhub.follow.repository.FollowedBusinessRepository;
import za.co.unilinkhub.listing.domain.Listing;
import za.co.unilinkhub.listing.domain.ListingStatus;
import za.co.unilinkhub.listing.repository.ListingRepository;
import za.co.unilinkhub.notification.application.NotificationService;
import za.co.unilinkhub.saved.application.SavedListingService;
import za.co.unilinkhub.user.application.UserDTO;
import za.co.unilinkhub.user.application.UserService;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BusinessService {

    private final BusinessRepository businessRepository;
    private final ListingRepository listingRepository;
    private final UserService userService;
    private final SavedListingService savedListingService;
    private final FollowedBusinessRepository followedBusinessRepository;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    public BusinessDTO create(UUID ownerId, String businessName, String description, String category) {
        // Becoming a seller and registering a first business happen together for the MVP flow.
        userService.becomeSeller(ownerId);
        Business business = Business.create(ownerId, businessName, description, category);
        return BusinessDTO.from(businessRepository.save(business));
    }

    public BusinessDTO update(UUID businessId, UUID requesterId, String businessName, String description,
                               String category, String imageUrl) {
        Business business = findOwned(businessId, requesterId);
        business.updateDetails(businessName, description, category, imageUrl);
        return BusinessDTO.from(businessRepository.save(business));
    }

    public BusinessDTO requestVerification(UUID businessId, UUID requesterId) {
        Business business = findOwned(businessId, requesterId);
        business.requestVerification();
        return BusinessDTO.from(businessRepository.save(business));
    }

    public BusinessDTO getById(UUID id) {
        return BusinessDTO.from(findById(id));
    }

    public ProviderProfileDTO getProviderProfile(UUID businessId) {
        return toProviderProfile(findById(businessId));
    }

    public List<BusinessDTO> getByOwner(UUID ownerId) {
        return businessRepository.findByOwnerId(ownerId).stream().map(BusinessDTO::from).toList();
    }

    public List<ProviderProfileDTO> listPublic(String keyword, String category, boolean verifiedOnly) {
        List<Business> businesses = businessRepository.findByVerificationStatusNot(VerificationStatus.REJECTED);

        return businesses.stream()
                .filter(b -> !verifiedOnly || b.getVerificationStatus() == VerificationStatus.VERIFIED)
                .filter(b -> category == null || category.isBlank() || b.getCategory().equalsIgnoreCase(category))
                .filter(b -> keyword == null || keyword.isBlank()
                        || b.getBusinessName().toLowerCase().contains(keyword.toLowerCase())
                        || b.getDescription().toLowerCase().contains(keyword.toLowerCase()))
                .map(this::toProviderProfile)
                .sorted(Comparator.comparing(ProviderProfileDTO::businessName))
                .toList();
    }

    private ProviderProfileDTO toProviderProfile(Business business) {
        UserDTO owner = userService.getById(business.getOwnerId());
        List<Listing> listings = listingRepository.findByBusinessId(business.getId());

        long activeCount = listings.stream().filter(l -> l.getStatus() == ListingStatus.ACTIVE).count();
        long totalViews = listings.stream().mapToLong(Listing::getViewCount).sum();

        return new ProviderProfileDTO(
                business.getId(), business.getBusinessName(), business.getDescription(), business.getCategory(),
                business.getVerificationStatus().name(), business.getImageUrl(), business.getOwnerId(),
                owner.firstName() + " " + owner.lastName(), activeCount, totalViews, business.getCreatedAt()
        );
    }

    public List<AdminBusinessView> recentlyDecided(int limit) {
        return businessRepository.findByVerificationStatusNot(VerificationStatus.PENDING).stream()
                .sorted(Comparator.comparing(Business::getUpdatedAt).reversed())
                .limit(limit)
                .map(this::toAdminView)
                .toList();
    }

    public BusinessDTO verify(UUID businessId, UUID adminId) {
        Business business = findById(businessId);
        business.verify();
        BusinessDTO dto = BusinessDTO.from(businessRepository.save(business));
        auditLogService.record(adminName(adminId), "BUSINESS", "Verified business \"" + business.getBusinessName() + "\"");
        notificationService.notify(business.getOwnerId(), "BUSINESS",
                "Your business \"" + business.getBusinessName() + "\" was verified!");
        return dto;
    }

    public BusinessDTO reject(UUID businessId, String reason, UUID adminId) {
        Business business = findById(businessId);
        business.reject(reason);
        BusinessDTO dto = BusinessDTO.from(businessRepository.save(business));
        auditLogService.record(adminName(adminId), "BUSINESS", "Rejected business \"" + business.getBusinessName() + "\""
                + (reason != null && !reason.isBlank() ? " — reason: " + reason : ""));
        notificationService.notify(business.getOwnerId(), "BUSINESS",
                "Your business \"" + business.getBusinessName() + "\" was not approved"
                        + (reason != null && !reason.isBlank() ? ": " + reason : "."));
        return dto;
    }

    public BusinessContactDTO getContact(UUID businessId) {
        Business business = findById(businessId);
        UserDTO owner = userService.getById(business.getOwnerId());
        return new BusinessContactDTO(owner.email(), owner.phoneNumber());
    }

    private String adminName(UUID adminId) {
        UserDTO admin = userService.getById(adminId);
        return admin.firstName() + " " + admin.lastName();
    }

    public List<AdminBusinessView> listForAdmin(String status, String keyword) {
        List<Business> businesses = (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status))
                ? businessRepository.findAll()
                : businessRepository.findByVerificationStatus(VerificationStatus.valueOf(status.toUpperCase()));

        if (keyword != null && !keyword.isBlank()) {
            String needle = keyword.toLowerCase();
            businesses = businesses.stream()
                    .filter(b -> b.getBusinessName().toLowerCase().contains(needle)
                            || b.getCategory().toLowerCase().contains(needle))
                    .toList();
        }

        return businesses.stream()
                .sorted(Comparator.comparing(Business::getCreatedAt).reversed())
                .map(this::toAdminView)
                .toList();
    }

    public BusinessStatsDTO getStats(UUID businessId, UUID requesterId) {
        Business business = findOwned(businessId, requesterId);
        List<Listing> listings = listingRepository.findByBusinessId(business.getId());

        long activeListings = listings.stream().filter(l -> l.getStatus() == ListingStatus.ACTIVE).count();
        long totalViews = listings.stream().mapToLong(Listing::getViewCount).sum();
        long totalSaves = listings.stream().mapToLong(l -> savedListingService.countSaves(l.getId())).sum();
        long followerCount = followedBusinessRepository.countByBusinessId(business.getId());

        return new BusinessStatsDTO(listings.size(), activeListings, totalViews, totalSaves, followerCount);
    }

    public List<ProviderProfileDTO> listSimilar(UUID businessId) {
        Business business = findById(businessId);
        return listPublic(null, business.getCategory(), false).stream()
                .filter(p -> !p.businessId().equals(businessId))
                .limit(4)
                .toList();
    }

    private Business findById(UUID id) {
        return businessRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));
    }

    private Business findOwned(UUID businessId, UUID requesterId) {
        Business business = findById(businessId);
        if (!business.getOwnerId().equals(requesterId)) {
            throw new UnauthorizedException("You do not own this business");
        }
        return business;
    }

    private AdminBusinessView toAdminView(Business business) {
        return AdminBusinessView.from(business, userService.getById(business.getOwnerId()));
    }
}
