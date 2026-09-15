package za.co.unilinkhub.promo.repository;

import za.co.unilinkhub.promo.domain.PromoCode;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PromoCodeRepository {

    PromoCode save(PromoCode promoCode);

    Optional<PromoCode> findById(UUID id);

    List<PromoCode> findByBusinessId(UUID businessId);

    Optional<PromoCode> findByBusinessIdAndCode(UUID businessId, String code);

    List<PromoCode> findAll();
}
