package com.lilamaris.lilboard.kernel.pagination;

import com.lilamaris.lilboard.kernel.condition.NumberPrecondition;

/** page는 0부터 시작하며 size는 조회할 최대 항목 수다. */
public record OffsetPageRequest(int page, int size) {
    public OffsetPageRequest {
        NumberPrecondition.requireNonNegative(page);
        NumberPrecondition.requirePositive(size);
    }

    public static OffsetPageRequest of(int page, int size) {
        return new OffsetPageRequest(page, size);
    }

    public long offset() {
        return (long) page * size;
    }
}
