package za.co.unilinkhub.booking.application;

import lombok.RequiredArgsConstructor;
import za.co.unilinkhub.booking.domain.Booking;
import za.co.unilinkhub.booking.domain.BookingStatus;
import za.co.unilinkhub.booking.repository.BookingRepository;
import za.co.unilinkhub.business.domain.Business;
import za.co.unilinkhub.business.repository.BusinessRepository;
import za.co.unilinkhub.common.exception.BadRequestException;
import za.co.unilinkhub.common.exception.ResourceNotFoundException;
import za.co.unilinkhub.common.exception.UnauthorizedException;
import za.co.unilinkhub.listing.domain.Listing;
import za.co.unilinkhub.listing.domain.Service;
import za.co.unilinkhub.listing.repository.ListingRepository;
import za.co.unilinkhub.notification.application.NotificationService;
import za.co.unilinkhub.user.domain.User;
import za.co.unilinkhub.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ListingRepository listingRepository;
    private final BusinessRepository businessRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public BookingSummaryView request(UUID buyerId, UUID listingId, LocalDateTime preferredAt, String note) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));
        if (!(listing instanceof Service)) {
            throw new BadRequestException("Only service listings can be booked");
        }
        Business business = businessRepository.findById(listing.getBusinessId())
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));

        Booking booking = Booking.request(listingId, business.getId(), buyerId, preferredAt, note);
        Booking saved = bookingRepository.save(booking);

        notificationService.notify(business.getOwnerId(), "BOOKING",
                "New booking request for \"" + listing.getName() + "\"");

        return toView(saved);
    }

    public List<BookingSummaryView> listMine(UUID buyerId) {
        return bookingRepository.findByBuyerId(buyerId).stream()
                .sorted(Comparator.comparing(Booking::getCreatedAt).reversed())
                .map(this::toView)
                .toList();
    }

    public List<BookingSummaryView> listForSeller(UUID sellerId) {
        List<UUID> businessIds = businessRepository.findByOwnerId(sellerId).stream().map(Business::getId).toList();
        return businessIds.stream()
                .flatMap(id -> bookingRepository.findByBusinessId(id).stream())
                .sorted(Comparator.comparing(Booking::getCreatedAt).reversed())
                .map(this::toView)
                .toList();
    }

    public BookingSummaryView accept(UUID bookingId, UUID sellerId) {
        Booking booking = findOwned(bookingId, sellerId);
        booking.accept();
        Booking saved = bookingRepository.save(booking);
        notifyBuyer(saved, "accepted");
        return toView(saved);
    }

    public BookingSummaryView decline(UUID bookingId, UUID sellerId, String reason) {
        Booking booking = findOwned(bookingId, sellerId);
        booking.decline(reason);
        Booking saved = bookingRepository.save(booking);
        notifyBuyer(saved, "declined");
        return toView(saved);
    }

    public BookingStatsDTO adminStats() {
        List<Booking> all = bookingRepository.findAll();
        long pending = bookingRepository.countByStatus(BookingStatus.PENDING);
        long accepted = bookingRepository.countByStatus(BookingStatus.ACCEPTED);
        long declined = bookingRepository.countByStatus(BookingStatus.DECLINED);

        Map<UUID, Long> countsByListing = all.stream()
                .collect(Collectors.groupingBy(Booking::getListingId, Collectors.counting()));

        List<BookingStatsDTO.TopService> mostBooked = countsByListing.entrySet().stream()
                .sorted(Map.Entry.<UUID, Long>comparingByValue().reversed())
                .limit(5)
                .map(e -> {
                    Listing listing = listingRepository.findById(e.getKey()).orElse(null);
                    String listingName = listing == null ? "Deleted listing" : listing.getName();
                    String businessName = listing == null ? "-" : businessRepository.findById(listing.getBusinessId())
                            .map(Business::getBusinessName).orElse("Unknown business");
                    return new BookingStatsDTO.TopService(listingName, businessName, e.getValue());
                })
                .toList();

        return new BookingStatsDTO(all.size(), pending, accepted, declined, mostBooked);
    }

    private void notifyBuyer(Booking booking, String outcome) {
        String listingName = listingRepository.findById(booking.getListingId()).map(Listing::getName).orElse("your booking");
        notificationService.notify(booking.getBuyerId(), "BOOKING",
                "Your booking request for \"" + listingName + "\" was " + outcome
                        + (booking.getDeclineReason() != null && !booking.getDeclineReason().isBlank()
                        ? ": " + booking.getDeclineReason() : ""));
    }

    private Booking findOwned(UUID bookingId, UUID sellerId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        Business business = businessRepository.findById(booking.getBusinessId())
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));
        if (!business.getOwnerId().equals(sellerId)) {
            throw new UnauthorizedException("You do not own this business");
        }
        return booking;
    }

    private BookingSummaryView toView(Booking booking) {
        String listingName = listingRepository.findById(booking.getListingId()).map(Listing::getName).orElse("Deleted listing");
        String businessName = businessRepository.findById(booking.getBusinessId()).map(Business::getBusinessName).orElse("Unknown business");
        String buyerName = userRepository.findById(booking.getBuyerId()).map(User::getFullName).orElse("Deleted account");
        return BookingSummaryView.of(booking, listingName, businessName, buyerName);
    }
}
