package za.co.unilinkhub.listing.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import za.co.unilinkhub.listing.application.ListingDTO;
import za.co.unilinkhub.listing.application.ListingService;
import za.co.unilinkhub.saved.application.SavedListingService;
import za.co.unilinkhub.security.CurrentUser;
import za.co.unilinkhub.stockalert.application.StockAlertService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/listings")
@RequiredArgsConstructor
public class ListingController {

    private final ListingService listingService;
    private final SavedListingService savedListingService;
    private final StockAlertService stockAlertService;

    public record CreateProductRequest(
            @NotNull UUID businessId, @NotBlank String name, @NotBlank String description,
            @NotBlank String category, @NotNull @Positive BigDecimal price,
            Integer stockQuantity, String imageUrl
    ) {
    }

    public record CreateServiceRequest(
            @NotNull UUID businessId, @NotBlank String name, @NotBlank String description,
            @NotBlank String category, @NotNull @Positive BigDecimal price,
            Integer durationMinutes, String availabilitySchedule
    ) {
    }

    public record UpdateListingRequest(String name, String description, String category, BigDecimal price,
                                        Integer stockQuantity, String imageUrl, Integer lowStockThreshold,
                                        Integer durationMinutes, String availabilitySchedule, String status) {
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public ListingDTO createProduct(@CurrentUser UUID userId, @Valid @RequestBody CreateProductRequest request) {
        return listingService.createProduct(userId, request.businessId(), request.name(), request.description(),
                request.category(), request.price(), request.stockQuantity(), request.imageUrl());
    }

    @PostMapping("/services")
    @ResponseStatus(HttpStatus.CREATED)
    public ListingDTO createService(@CurrentUser UUID userId, @Valid @RequestBody CreateServiceRequest request) {
        return listingService.createService(userId, request.businessId(), request.name(), request.description(),
                request.category(), request.price(), request.durationMinutes(), request.availabilitySchedule());
    }

    @PatchMapping("/{id}")
    public ListingDTO update(@CurrentUser UUID userId, @PathVariable UUID id, @RequestBody UpdateListingRequest request) {
        return listingService.update(userId, id, request.name(), request.description(), request.category(),
                request.price(), request.stockQuantity(), request.imageUrl(), request.lowStockThreshold(),
                request.durationMinutes(), request.availabilitySchedule(), request.status());
    }

    @PostMapping("/{id}/deactivate")
    public void deactivate(@CurrentUser UUID userId, @PathVariable UUID id) {
        listingService.deactivate(userId, id);
    }

    @PostMapping("/{id}/reactivate")
    public void reactivate(@CurrentUser UUID userId, @PathVariable UUID id) {
        listingService.reactivate(userId, id);
    }

    @GetMapping("/{id}")
    public ListingDTO getById(@PathVariable UUID id) {
        ListingDTO listing = listingService.getById(id);
        return listing.withSavedCount(savedListingService.countSaves(id));
    }

    @GetMapping
    public List<ListingDTO> search(@RequestParam(required = false) String category,
                                    @RequestParam(required = false) String keyword,
                                    @RequestParam(required = false) BigDecimal minPrice,
                                    @RequestParam(required = false) BigDecimal maxPrice,
                                    @RequestParam(required = false) String type,
                                    @RequestParam(defaultValue = "false") boolean verifiedOnly,
                                    @RequestParam(required = false) String sort) {
        return listingService.search(category, keyword, minPrice, maxPrice, type, verifiedOnly, sort);
    }

    @GetMapping("/business/{businessId}")
    public List<ListingDTO> byBusiness(@PathVariable UUID businessId) {
        return listingService.byBusiness(businessId);
    }

    @PostMapping("/{id}/save")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void save(@CurrentUser UUID userId, @PathVariable UUID id) {
        savedListingService.save(userId, id);
    }

    @DeleteMapping("/{id}/save")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unsave(@CurrentUser UUID userId, @PathVariable UUID id) {
        savedListingService.unsave(userId, id);
    }

    @GetMapping("/saved/mine")
    public List<ListingDTO> savedMine(@CurrentUser UUID userId) {
        return savedListingService.mine(userId);
    }

    @GetMapping("/mine")
    public List<ListingDTO> mine(@CurrentUser UUID userId) {
        return listingService.listMine(userId);
    }

    @PostMapping("/{id}/notify-me")
    public Map<String, Boolean> toggleNotifyMe(@CurrentUser UUID userId, @PathVariable UUID id) {
        return Map.of("subscribed", stockAlertService.toggle(userId, id));
    }
}
