package za.co.unilinkhub.platform.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import za.co.unilinkhub.business.domain.VerificationStatus;
import za.co.unilinkhub.business.repository.BusinessRepository;
import za.co.unilinkhub.listing.domain.ListingStatus;
import za.co.unilinkhub.listing.repository.ListingRepository;
import za.co.unilinkhub.ordering.domain.OrderStatus;
import za.co.unilinkhub.ordering.repository.OrderRepository;
import za.co.unilinkhub.user.domain.AccountStatus;
import za.co.unilinkhub.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class PublicStatsService {

    private final UserRepository userRepository;
    private final BusinessRepository businessRepository;
    private final ListingRepository listingRepository;
    private final OrderRepository orderRepository;

    public PublicStatsDTO compute() {
        return new PublicStatsDTO(
                userRepository.findByAccountStatus(AccountStatus.ACTIVE).size(),
                businessRepository.findByVerificationStatus(VerificationStatus.VERIFIED).size(),
                listingRepository.countByStatus(ListingStatus.ACTIVE),
                orderRepository.countByStatus(OrderStatus.COMPLETED)
        );
    }
}
