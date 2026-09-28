package za.co.unilinkhub.admin.web;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import za.co.unilinkhub.admin.application.AdminStatsDTO;
import za.co.unilinkhub.admin.application.AdminStatsService;
import za.co.unilinkhub.admin.application.AdminUserDetailDTO;
import za.co.unilinkhub.admin.application.AdminUserDetailService;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class AdminStatsController {

    private final AdminStatsService adminStatsService;
    private final AdminUserDetailService adminUserDetailService;

    @GetMapping("/api/admin/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public AdminStatsDTO stats() {
        return adminStatsService.compute();
    }

    @GetMapping("/api/admin/users/{id}/detail")
    @PreAuthorize("hasRole('ADMIN')")
    public AdminUserDetailDTO userDetail(@PathVariable UUID id) {
        return adminUserDetailService.get(id);
    }
}
