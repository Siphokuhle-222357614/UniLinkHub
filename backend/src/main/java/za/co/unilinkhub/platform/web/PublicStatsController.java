package za.co.unilinkhub.platform.web;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import za.co.unilinkhub.platform.application.PublicStatsDTO;
import za.co.unilinkhub.platform.application.PublicStatsService;

@RestController
@RequiredArgsConstructor
public class PublicStatsController {

    private final PublicStatsService publicStatsService;

    @GetMapping("/api/stats/public")
    public PublicStatsDTO publicStats() {
        return publicStatsService.compute();
    }
}
