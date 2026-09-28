package za.co.unilinkhub.admin.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import za.co.unilinkhub.business.application.BusinessService;
import za.co.unilinkhub.report.application.ReportService;
import za.co.unilinkhub.user.application.UserService;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminUserDetailService {

    private final UserService userService;
    private final BusinessService businessService;
    private final ReportService reportService;

    public AdminUserDetailDTO get(UUID userId) {
        return new AdminUserDetailDTO(
                userService.getById(userId),
                businessService.getByOwner(userId),
                reportService.myReports(userId),
                reportService.reportsAgainst(userId)
        );
    }
}
