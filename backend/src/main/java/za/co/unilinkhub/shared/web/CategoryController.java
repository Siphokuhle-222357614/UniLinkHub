package za.co.unilinkhub.shared.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * A fixed suggested taxonomy for listing/business categories. Category itself stays a free-text
 * field on Listing and Business (no schema change, no breaking existing free-text values) - this
 * just gives the frontend a consistent set of options to offer in a dropdown instead of letting
 * "Printing" and "printing" drift into two different categories.
 */
@RestController
public class CategoryController {

    private static final List<String> CATEGORIES = List.of(
            "Printing", "Tutoring", "Food", "Design", "Hair & Beauty", "Tech", "Events", "Other"
    );

    @GetMapping("/api/categories")
    public List<String> list() {
        return CATEGORIES;
    }
}
