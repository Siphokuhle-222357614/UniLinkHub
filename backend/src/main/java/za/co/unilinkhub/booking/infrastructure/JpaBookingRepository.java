package za.co.unilinkhub.booking.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.unilinkhub.booking.domain.Booking;
import za.co.unilinkhub.booking.domain.BookingStatus;

import java.util.List;
import java.util.UUID;

public interface JpaBookingRepository extends JpaRepository<Booking, UUID>, za.co.unilinkhub.booking.repository.BookingRepository {
    List<Booking> findByBuyerId(UUID buyerId);
    List<Booking> findByBusinessId(UUID businessId);
    long countByStatus(BookingStatus status);
}
