package za.co.unilinkhub.common.web;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/** One page of results plus what the UI needs to show "Load more" and "123 listings". */
public record PageResponse<T>(List<T> items, int page, int size, long totalItems, int totalPages, boolean hasNext) {

    public static <E, T> PageResponse<T> of(Page<E> page, Function<List<E>, List<T>> mapper) {
        return new PageResponse<>(mapper.apply(page.getContent()), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.hasNext());
    }
}
