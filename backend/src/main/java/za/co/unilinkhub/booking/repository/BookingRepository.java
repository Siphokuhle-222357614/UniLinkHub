package za.co.unilinkhub.booking.repository;

import za.co.unilinkhub.booking.domain.Booking;
import za.co.unilinkhub.booking.domain.BookingStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository {
    Booking save(Booking booking);
    Optional<Booking> findById(UUID id);
    List<Booking> findByBuyerId(UUID buyerId);
    List<Booking> findByBusinessId(UUID businessId);
    List<Booking> findAll();
    long countByStatus(BookingStatus status);
}
