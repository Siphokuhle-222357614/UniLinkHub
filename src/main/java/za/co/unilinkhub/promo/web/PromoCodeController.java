package za.co.unilinkhub.promo.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import za.co.unilinkhub.promo.application.PromoCodeDTO;
import za.co.unilinkhub.promo.application.PromoCodeService;
import za.co.unilinkhub.promo.application.PromoStatsDTO;
import za.co.unilinkhub.security.CurrentUser;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class PromoCodeController {

    private final PromoCodeService promoCodeService;

    public record CreatePromoCodeRequest(@NotBlank String code, @NotBlank String discountType,
                                          @NotNull @Positive BigDecimal discountValue,
                                          UUID scopeListingId, LocalDateTime expiresAt) {
    }

    public record SetActiveRequest(boolean active) {
    }

    @PostMapping("/api/businesses/{businessId}/promo-codes")
    @ResponseStatus(HttpStatus.CREATED)
    public PromoCodeDTO create(@CurrentUser UUID sellerId, @PathVariable UUID businessId,
                                @Valid @RequestBody CreatePromoCodeRequest request) {
        return promoCodeService.create(sellerId, businessId, request.code(), request.discountType(),
                request.discountValue(), request.scopeListingId(), request.expiresAt());
    }

    @GetMapping("/api/businesses/{businessId}/promo-codes")
    public List<PromoCodeDTO> forBusiness(@CurrentUser UUID sellerId, @PathVariable UUID businessId) {
        return promoCodeService.listForBusiness(sellerId, businessId);
    }

    @PatchMapping("/api/promo-codes/{id}/active")
    public PromoCodeDTO setActive(@CurrentUser UUID sellerId, @PathVariable UUID id, @RequestBody SetActiveRequest request) {
        return promoCodeService.setActive(sellerId, id, request.active());
    }

    @GetMapping("/api/listings/{listingId}/promo")
    public PromoCodeDTO activePromoForListing(@PathVariable UUID listingId) {
        return promoCodeService.bestActiveForListing(listingId).orElse(null);
    }

    @GetMapping("/api/admin/promo-codes/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public PromoStatsDTO adminStats() {
        return promoCodeService.adminStats();
    }
}
