package za.co.unilinkhub.moderation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.unilinkhub.audit.application.AuditLogService;
import za.co.unilinkhub.business.domain.Business;
import za.co.unilinkhub.business.repository.BusinessRepository;
import za.co.unilinkhub.common.exception.BadRequestException;
import za.co.unilinkhub.common.exception.ResourceNotFoundException;
import za.co.unilinkhub.common.web.PageResponse;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import za.co.unilinkhub.listing.application.ListingDTO;
import za.co.unilinkhub.listing.domain.Listing;
import za.co.unilinkhub.listing.repository.ListingRepository;
import za.co.unilinkhub.notification.application.NotificationService;
import za.co.unilinkhub.report.domain.Report;
import za.co.unilinkhub.report.domain.ReportStatus;
import za.co.unilinkhub.report.domain.ReportTargetType;
import za.co.unilinkhub.report.repository.ReportRepository;
import za.co.unilinkhub.user.domain.User;
import za.co.unilinkhub.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** The admin console's view of every listing on the platform, and the power to take one down. */
@Service
@RequiredArgsConstructor
public class ListingModerationService {

    private final ListingRepository listingRepository;
    private final BusinessRepository businessRepository;
    private final UserRepository userRepository;
    private final ReportRepository reportRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    public record AdminListingView(ListingDTO listing, String businessName, String businessVerificationStatus,
                                   UUID ownerId, String ownerName, String ownerEmail, long openReports) {
    }

    /**
     * @param status ALL, ACTIVE, INACTIVE, SOLD_OUT or REMOVED (taken down by an admin)
     */
    public PageResponse<AdminListingView> list(String status, String keyword, UUID businessId, int page, int size) {
        Map<UUID, Business> businesses = businessRepository.findAll().stream()
                .collect(Collectors.toMap(Business::getId, Function.identity()));
        Map<UUID, User> owners = userRepository.findAll().stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        Map<UUID, Long> openReports = reportRepository.findAll().stream()
                .filter(r -> r.getTargetType() == ReportTargetType.LISTING)
                .filter(r -> r.getStatus() == ReportStatus.OPEN || r.getStatus() == ReportStatus.UNDER_REVIEW)
                .collect(Collectors.groupingBy(Report::getTargetId, Collectors.counting()));
        String needle = keyword == null ? "" : keyword.trim().toLowerCase();
        String wanted = status == null ? "ALL" : status.trim().toUpperCase();

        List<AdminListingView> all = listingRepository.findAll().stream()
                .filter(l -> businessId == null || l.getBusinessId().equals(businessId))
                .filter(l -> switch (wanted) {
                    case "ALL", "" -> true;
                    case "REMOVED" -> l.isTakenDown();
                    default -> !l.isTakenDown() && l.getStatus().name().equals(wanted);
                })
                .map(l -> {
                    Business b = businesses.get(l.getBusinessId());
                    User owner = b == null ? null : owners.get(b.getOwnerId());
                    return new AdminListingView(ListingDTO.from(l),
                            b == null ? "Unknown business" : b.getBusinessName(),
                            b == null ? null : b.getVerificationStatus().name(),
                            owner == null ? null : owner.getId(),
                            owner == null ? "Unknown" : owner.getFullName(),
                            owner == null ? null : owner.getEmail(),
                            openReports.getOrDefault(l.getId(), 0L));
                })
                .filter(v -> needle.isEmpty()
                        || v.listing().name().toLowerCase().contains(needle)
                        || v.businessName().toLowerCase().contains(needle)
                        || v.ownerName().toLowerCase().contains(needle))
                // Reported listings first - they're the ones an admin most needs to look at. That
                // ordering needs the report counts, so this page is cut in memory, not by the database.
                .sorted(Comparator.comparingLong(AdminListingView::openReports).reversed()
                        .thenComparing(v -> v.listing().createdAt(), Comparator.reverseOrder()))
                .toList();
        return PageResponse.of(new PageImpl<>(slice(all, page, size), PageRequest.of(Math.max(0, page), clampSize(size)), all.size()),
                items -> items);
    }

    private static int clampSize(int size) {
        return Math.min(Math.max(1, size), 100);
    }

    private static <T> List<T> slice(List<T> all, int page, int size) {
        int from = Math.min(all.size(), Math.max(0, page) * clampSize(size));
        return all.subList(from, Math.min(all.size(), from + clampSize(size)));
    }

    @Transactional
    public ListingDTO takeDown(UUID listingId, UUID adminId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BadRequestException("Please give a reason for taking this listing down. The seller is shown it, so "
                    + "they understand which rule was broken.");
        }
        Listing listing = findListing(listingId);
        if (listing.isTakenDown()) {
            throw new BadRequestException("This listing has already been taken down.");
        }
        listing.takeDown(reason.trim(), LocalDateTime.now());
        Listing saved = listingRepository.save(listing);

        Business business = businessRepository.findById(listing.getBusinessId()).orElse(null);
        if (business != null) {
            notificationService.notify(business.getOwnerId(), "MODERATION",
                    "An admin removed your listing \"" + listing.getName() + "\" because it broke the marketplace rules: "
                            + reason.trim() + ". Repeated or serious breaches (like selling alcohol, drugs or weapons) can lead "
                            + "to your account being suspended and being reported to CPUT residence management.");
        }
        auditLogService.record(adminName(adminId), "LISTING", "Took down listing \"" + listing.getName() + "\""
                + (business != null ? " by " + business.getBusinessName() : "") + " - reason: " + reason.trim());
        return ListingDTO.from(saved);
    }

    @Transactional
    public ListingDTO restore(UUID listingId, UUID adminId) {
        Listing listing = findListing(listingId);
        if (!listing.isTakenDown()) {
            throw new BadRequestException("This listing hasn't been taken down, so there's nothing to restore.");
        }
        listing.restoreAfterTakedown();
        Listing saved = listingRepository.save(listing);
        businessRepository.findById(listing.getBusinessId()).ifPresent(business ->
                notificationService.notify(business.getOwnerId(), "MODERATION",
                        "An admin restored your listing \"" + listing.getName() + "\". It's still hidden - switch it back on "
                                + "from My listings when you're ready."));
        auditLogService.record(adminName(adminId), "LISTING", "Restored listing \"" + listing.getName() + "\"");
        return ListingDTO.from(saved);
    }

    private Listing findListing(UUID id) {
        return listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("We couldn't find that listing. It may have been deleted."));
    }

    private String adminName(UUID adminId) {
        return userRepository.findById(adminId).map(User::getFullName).orElse("An admin");
    }
}
