package za.co.unilinkhub.admin.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import za.co.unilinkhub.business.domain.VerificationStatus;
import za.co.unilinkhub.business.repository.BusinessRepository;
import za.co.unilinkhub.listing.domain.ListingStatus;
import za.co.unilinkhub.listing.repository.ListingRepository;
import za.co.unilinkhub.report.application.ReportService;
import za.co.unilinkhub.user.domain.AccountStatus;
import za.co.unilinkhub.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class AdminStatsService {

    private final UserRepository userRepository;
    private final BusinessRepository businessRepository;
    private final ListingRepository listingRepository;
    private final ReportService reportService;

    public AdminStatsDTO compute() {
        long totalStudents = userRepository.countAll();
        long pendingAccounts = userRepository.findByAccountStatus(AccountStatus.PENDING_VERIFICATION).size();

        AdminStatsDTO.BusinessCounts businesses = new AdminStatsDTO.BusinessCounts(
                businessRepository.findByVerificationStatus(VerificationStatus.PENDING).size(),
                businessRepository.findByVerificationStatus(VerificationStatus.VERIFIED).size(),
                businessRepository.findByVerificationStatus(VerificationStatus.REJECTED).size()
        );

        AdminStatsDTO.ListingCounts listings = new AdminStatsDTO.ListingCounts(
                listingRepository.countByStatus(ListingStatus.ACTIVE),
                listingRepository.countByStatus(ListingStatus.INACTIVE),
                listingRepository.countByStatus(ListingStatus.SOLD_OUT)
        );

        return new AdminStatsDTO(totalStudents, pendingAccounts, businesses, listings, reportService.counts());
    }
}
