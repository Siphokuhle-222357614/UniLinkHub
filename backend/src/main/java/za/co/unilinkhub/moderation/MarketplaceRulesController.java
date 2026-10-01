package za.co.unilinkhub.moderation;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The restricted-items list, served to the frontend's Marketplace Rules page and seller sign-up so
 * the rules students read are exactly the rules the backend enforces.
 */
@RestController
public class MarketplaceRulesController {

    public record RestrictedCategoryView(String key, String label, String description, List<String> examples) {
    }

    @GetMapping("/api/rules/restricted-items")
    public List<RestrictedCategoryView> restrictedItems() {
        return RestrictedItemsPolicy.CATEGORIES.stream()
                .map(c -> new RestrictedCategoryView(c.key(), c.label(), c.description(), c.examples()))
                .toList();
    }
}
