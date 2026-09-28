package za.co.unilinkhub.admin.application;

import za.co.unilinkhub.business.application.BusinessDTO;
import za.co.unilinkhub.report.application.ReportSummaryView;
import za.co.unilinkhub.user.application.UserDTO;

import java.util.List;

public record AdminUserDetailDTO(
        UserDTO user,
        List<BusinessDTO> businesses,
        List<ReportSummaryView> reportsFiled,
        List<ReportSummaryView> reportsReceived
) {
}
