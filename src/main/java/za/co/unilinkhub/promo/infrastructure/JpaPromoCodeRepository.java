package za.co.unilinkhub.promo.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.unilinkhub.promo.domain.PromoCode;
import za.co.unilinkhub.promo.repository.PromoCodeRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaPromoCodeRepository extends JpaRepository<PromoCode, UUID>, PromoCodeRepository {
    @Override
    List<PromoCode> findByBusinessId(UUID businessId);

    @Override
    Optional<PromoCode> findByBusinessIdAndCode(UUID businessId, String code);
}
