package api.common;

import java.util.List;
import java.util.function.Function;

public record CursorPage<T>(List<T> items, Integer nextAfterId) {

    public static <E, T> CursorPage<T> fromRows(List<E> rows, int limit, Function<E, T> toResponse,
                                                Function<E, Integer> getId) {
        boolean hasMore = rows.size() > limit;
        List<E> visibleRows = hasMore ? rows.subList(0, limit) : rows;
        Integer nextAfterId = hasMore ? getId.apply(visibleRows.getLast()) : null;
        return new CursorPage<>(visibleRows.stream().map(toResponse).toList(), nextAfterId);
    }
}
