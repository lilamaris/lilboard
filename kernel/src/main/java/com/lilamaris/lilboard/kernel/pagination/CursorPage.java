package com.lilamaris.lilboard.kernel.pagination;

import com.lilamaris.lilboard.kernel.condition.NumberPrecondition;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/** nextCursor가 null이면 다음 페이지가 없다. */
public record CursorPage<T, I>(List<T> items, Cursor<I> nextCursor) {
    public CursorPage {
        items = List.copyOf(items);
    }

    public static <T, I> CursorPage<T, I> of(List<T> items, Cursor<I> nextCursor) {
        return new CursorPage<>(items, nextCursor);
    }

    /** 동일한 정렬 순서로 limit + 1개까지 조회한 결과를 페이지로 변환한다. */
    public static <T, I> CursorPage<T, I> from(
            List<T> fetched, int limit, Function<? super T, Cursor<I>> cursorExtractor) {
        NumberPrecondition.requirePositive(limit);
        Objects.requireNonNull(cursorExtractor, "cursorExtractor must not be null.");
        List<T> snapshot = List.copyOf(fetched);
        boolean hasNext = snapshot.size() > limit;
        List<T> items = snapshot.subList(0, Math.min(snapshot.size(), limit));
        Cursor<I> nextCursor =
                hasNext
                        ? Objects.requireNonNull(
                                cursorExtractor.apply(items.getLast()),
                                "nextCursor must not be null.")
                        : null;
        return new CursorPage<>(items, nextCursor);
    }

    public boolean hasNext() {
        return nextCursor != null;
    }
}
