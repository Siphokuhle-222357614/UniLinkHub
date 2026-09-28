package za.co.unilinkhub.business.repository;

import za.co.unilinkhub.business.domain.Business;
import za.co.unilinkhub.business.domain.VerificationStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BusinessRepository {

    Business save(Business business);

    Optional<Business> findById(UUID id);

    List<Business> findByOwnerId(UUID ownerId);

    List<Business> findByVerificationStatus(VerificationStatus status);

    List<Business> findByVerificationStatusNot(VerificationStatus status);

    List<Business> findAll();
}
