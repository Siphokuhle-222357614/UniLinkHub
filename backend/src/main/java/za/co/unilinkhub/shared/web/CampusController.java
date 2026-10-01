package za.co.unilinkhub.shared.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import za.co.unilinkhub.shared.domain.Campus;

import java.util.Arrays;
import java.util.List;

@RestController
public class CampusController {

    public record CampusView(String key, String label) {
    }

    @GetMapping("/api/campuses")
    public List<CampusView> list() {
        return Arrays.stream(Campus.values()).map(c -> new CampusView(c.name(), c.label())).toList();
    }
}
