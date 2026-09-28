package za.co.unilinkhub.listing.domain;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@DiscriminatorValue("SERVICE")
@Getter
@NoArgsConstructor
public class Service extends Listing {

    private Integer durationMinutes;
    private String availabilitySchedule;

    private Service(UUID businessId, String name, String description, String category,
                     BigDecimal price, Integer durationMinutes, String availabilitySchedule) {
        super(businessId, name, description, category, price);
        this.durationMinutes = durationMinutes;
        this.availabilitySchedule = availabilitySchedule;
    }

    public static Service create(UUID businessId, String name, String description, String category,
                                  BigDecimal price, Integer durationMinutes, String availabilitySchedule) {
        return new Service(businessId, name, description, category, price, durationMinutes, availabilitySchedule);
    }

    public void updateSchedule(String availabilitySchedule) {
        this.availabilitySchedule = availabilitySchedule;
    }

    public void updateDuration(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }
}
