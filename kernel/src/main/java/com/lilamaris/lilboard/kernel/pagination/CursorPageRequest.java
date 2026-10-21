package com.lilamaris.lilboard.kernel.pagination;

import com.lilamaris.lilboard.kernel.condition.NumberPrecondition;

/** 커서가 null이면 첫 페이지를 조회한다. size는 조회할 최대 항목 수다. */
public record CursorPageRequest<I>(Cursor<I> cursor, int size) {
    public CursorPageRequest {
        NumberPrecondition.requirePositive(size);
    }

    public static <I> CursorPageRequest<I> of(Cursor<I> cursor, int size) {
        return new CursorPageRequest<>(cursor, size);
    }

    public boolean isFirstPage() {
        return cursor == null;
    }
}
