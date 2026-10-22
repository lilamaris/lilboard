package com.lilamaris.lilboard.kernel.pagination;

import com.lilamaris.lilboard.kernel.condition.NumberPrecondition;
import java.util.List;

/** page는 0부터 시작한다. 빈 결과의 전체 페이지 수는 0이다. */
public record OffsetPage<T>(List<T> items, int page, int size, long totalElements) {
    public OffsetPage {
        items = List.copyOf(items);
        NumberPrecondition.requireNonNegative(page);
        NumberPrecondition.requirePositive(size);
        NumberPrecondition.requireNonNegative(totalElements);
        if (items.size() > size) {
            throw new IllegalArgumentException("items count must not exceed size.");
        }
    }

    public static <T> OffsetPage<T> of(List<T> items, int page, int size, long totalElements) {
        return new OffsetPage<>(items, page, size, totalElements);
    }

    public long totalPages() {
        return totalElements / size + (totalElements % size == 0 ? 0 : 1);
    }

    public boolean hasPrevious() {
        return page > 0;
    }

    public boolean hasNext() {
        return (long) page + 1 < totalPages();
    }
}
