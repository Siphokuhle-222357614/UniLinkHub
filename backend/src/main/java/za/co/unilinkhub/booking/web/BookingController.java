package za.co.unilinkhub.booking.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import za.co.unilinkhub.booking.application.BookingService;
import za.co.unilinkhub.booking.application.BookingStatsDTO;
import za.co.unilinkhub.booking.application.BookingSummaryView;
import za.co.unilinkhub.security.CurrentUser;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    public record RequestBookingRequest(@NotNull UUID listingId, @NotNull LocalDateTime preferredAt, String note) {
    }

    public record DeclineRequest(String reason) {
    }

    @PostMapping("/api/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingSummaryView request(@CurrentUser UUID buyerId, @Valid @RequestBody RequestBookingRequest request) {
        return bookingService.request(buyerId, request.listingId(), request.preferredAt(), request.note());
    }

    @GetMapping("/api/bookings/mine")
    public List<BookingSummaryView> mine(@CurrentUser UUID buyerId) {
        return bookingService.listMine(buyerId);
    }

    @GetMapping("/api/bookings/seller")
    public List<BookingSummaryView> seller(@CurrentUser UUID sellerId) {
        return bookingService.listForSeller(sellerId);
    }

    @PostMapping("/api/bookings/{id}/accept")
    public BookingSummaryView accept(@CurrentUser UUID sellerId, @PathVariable UUID id) {
        return bookingService.accept(id, sellerId);
    }

    @PostMapping("/api/bookings/{id}/decline")
    public BookingSummaryView decline(@CurrentUser UUID sellerId, @PathVariable UUID id, @RequestBody(required = false) DeclineRequest request) {
        return bookingService.decline(id, sellerId, request == null ? null : request.reason());
    }

    @GetMapping("/api/admin/bookings/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public BookingStatsDTO adminStats() {
        return bookingService.adminStats();
    }
}
