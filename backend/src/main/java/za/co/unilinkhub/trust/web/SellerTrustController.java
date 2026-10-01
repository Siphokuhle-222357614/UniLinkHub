package za.co.unilinkhub.trust.web;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import za.co.unilinkhub.trust.application.SellerTrust;
import za.co.unilinkhub.trust.application.SellerTrustService;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class SellerTrustController {

    private final SellerTrustService sellerTrustService;

    // Public, like the provider page it's shown on (covered by GET /api/businesses/** in SecurityConfig).
    @GetMapping("/api/businesses/{id}/trust")
    public SellerTrust trust(@PathVariable UUID id) {
        return sellerTrustService.forBusiness(id);
    }
}
