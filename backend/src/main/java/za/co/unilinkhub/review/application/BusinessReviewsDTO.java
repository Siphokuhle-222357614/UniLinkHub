package za.co.unilinkhub.review.application;

import java.util.List;
import java.util.Map;

public record BusinessReviewsDTO(
        double average,
        long total,
        Map<Integer, Long> distribution,
        List<ReviewView> reviews
) {
}
