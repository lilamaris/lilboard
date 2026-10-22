package com.lilamaris.lilboard.kernel.pagination;

import com.lilamaris.lilboard.kernel.condition.NumberPrecondition;

/** 커서가 null이면 첫 페이지를 조회한다. size는 조회할 최대 항목 수이며, 다음 페이지 확인용 추가 조회를 위해 Integer.MAX_VALUE보다 작아야 한다. */
public record CursorPageRequest<I>(Cursor<I> cursor, int size) {
    public CursorPageRequest {
        NumberPrecondition.requirePositive(size);
        NumberPrecondition.requireAtMost(size, Integer.MAX_VALUE - 1);
    }

    public static <I> CursorPageRequest<I> of(Cursor<I> cursor, int size) {
        return new CursorPageRequest<>(cursor, size);
    }

    public boolean isFirstPage() {
        return cursor == null;
    }
}
