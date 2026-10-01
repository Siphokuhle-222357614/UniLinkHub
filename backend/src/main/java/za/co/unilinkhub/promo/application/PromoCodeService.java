package za.co.unilinkhub.promo.application;

import za.co.unilinkhub.common.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import za.co.unilinkhub.business.domain.Business;
import za.co.unilinkhub.business.repository.BusinessRepository;
import za.co.unilinkhub.common.exception.ResourceNotFoundException;
import za.co.unilinkhub.listing.domain.Listing;
import za.co.unilinkhub.listing.repository.ListingRepository;
import za.co.unilinkhub.promo.domain.DiscountType;
import za.co.unilinkhub.promo.domain.PromoCode;
import za.co.unilinkhub.promo.repository.PromoCodeRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PromoCodeService {

    private final PromoCodeRepository promoCodeRepository;
    private final BusinessRepository businessRepository;
    private final ListingRepository listingRepository;

    public PromoCodeDTO create(UUID sellerId, UUID businessId, String code, String discountType,
                                BigDecimal discountValue, UUID scopeListingId, LocalDateTime expiresAt) {
        assertOwnership(businessId, sellerId);
        PromoCode promoCode = PromoCode.create(businessId, code.trim().toUpperCase(),
                DiscountType.valueOf(discountType.toUpperCase()), discountValue, scopeListingId, expiresAt);
        return toDTO(promoCodeRepository.save(promoCode));
    }

    public List<PromoCodeDTO> listForBusiness(UUID sellerId, UUID businessId) {
        assertOwnership(businessId, sellerId);
        return promoCodeRepository.findByBusinessId(businessId).stream()
                .sorted(Comparator.comparing(PromoCode::getCreatedAt).reversed())
                .map(this::toDTO)
                .toList();
    }

    public PromoCodeDTO setActive(UUID sellerId, UUID promoCodeId, boolean active) {
        PromoCode promoCode = findOwned(promoCodeId, sellerId);
        promoCode.setActive(active);
        return toDTO(promoCodeRepository.save(promoCode));
    }

    public Optional<PromoCodeDTO> bestActiveForListing(UUID listingId) {
        Listing listing = listingRepository.findById(listingId).orElse(null);
        if (listing == null) {
            return Optional.empty();
        }
        return promoCodeRepository.findByBusinessId(listing.getBusinessId()).stream()
                .filter(PromoCode::isUsable)
                .filter(p -> p.getScopeListingId() == null || p.getScopeListingId().equals(listingId))
                .findFirst()
                .map(this::toDTO);
    }

    /**
     * Validates a code against one business's cart lines and, if it applies, records the usage
     * and returns the discount amount. Silently returns zero for a missing/expired/mismatched
     * code rather than failing checkout - an invalid promo code just means no discount, not an
     * error, since a cart can span several businesses and the code may only be meant for one.
     */
    public BigDecimal applyIfValid(UUID businessId, List<UUID> listingIds, String code, BigDecimal subtotal) {
        if (code == null || code.isBlank()) {
            return BigDecimal.ZERO;
        }
        Optional<PromoCode> found = promoCodeRepository.findByBusinessIdAndCode(businessId, code.trim().toUpperCase());
        if (found.isEmpty()) {
            return BigDecimal.ZERO;
        }
        PromoCode promoCode = found.get();
        if (!promoCode.isUsable()) {
            return BigDecimal.ZERO;
        }
        if (promoCode.getScopeListingId() != null && !listingIds.contains(promoCode.getScopeListingId())) {
            return BigDecimal.ZERO;
        }
        BigDecimal discount = promoCode.computeDiscount(subtotal);
        promoCode.recordUsage(discount);
        promoCodeRepository.save(promoCode);
        return discount;
    }

    public PromoStatsDTO adminStats() {
        List<PromoCode> all = promoCodeRepository.findAll();
        long activeCount = all.stream().filter(PromoCode::isUsable).count();
        long totalRedemptions = all.stream().mapToLong(PromoCode::getUsageCount).sum();
        BigDecimal totalDiscount = all.stream().map(PromoCode::getTotalDiscountGiven).reduce(BigDecimal.ZERO, BigDecimal::add);

        List<PromoStatsDTO.PromoUsageRow> topUsed = all.stream()
                .sorted(Comparator.comparingInt(PromoCode::getUsageCount).reversed())
                .limit(5)
                .map(p -> new PromoStatsDTO.PromoUsageRow(
                        p.getCode(),
                        businessRepository.findById(p.getBusinessId()).map(Business::getBusinessName).orElse("Unknown business"),
                        discountLabel(p), p.getUsageCount(), p.isUsable()))
                .toList();

        return new PromoStatsDTO(activeCount, totalRedemptions, totalDiscount, topUsed);
    }

    private String discountLabel(PromoCode promoCode) {
        return promoCode.getDiscountType() == DiscountType.PERCENT
                ? promoCode.getDiscountValue().stripTrailingZeros().toPlainString() + "%"
                : "R" + promoCode.getDiscountValue().stripTrailingZeros().toPlainString();
    }

    private PromoCode findOwned(UUID promoCodeId, UUID sellerId) {
        PromoCode promoCode = promoCodeRepository.findById(promoCodeId)
                .orElseThrow(() -> new ResourceNotFoundException("We couldn't find that promo code."));
        assertOwnership(promoCode.getBusinessId(), sellerId);
        return promoCode;
    }

    private void assertOwnership(UUID businessId, UUID sellerId) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("We couldn't find that business. It may have been removed."));
        if (!business.getOwnerId().equals(sellerId)) {
            throw new ForbiddenException("Only the owner of this business can do this. You can only manage businesses you created yourself.");
        }
    }

    private PromoCodeDTO toDTO(PromoCode promoCode) {
        String scopeListingName = promoCode.getScopeListingId() == null ? null
                : listingRepository.findById(promoCode.getScopeListingId()).map(Listing::getName).orElse(null);
        return PromoCodeDTO.from(promoCode, scopeListingName);
    }
}
