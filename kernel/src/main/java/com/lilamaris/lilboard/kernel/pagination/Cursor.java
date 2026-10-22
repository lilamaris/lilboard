package com.lilamaris.lilboard.kernel.pagination;

import java.time.Instant;
import java.util.Objects;

/** 생성 시각과 고유 ID로 정렬한 목록에서 마지막으로 조회한 항목의 위치. */
public record Cursor<I>(Instant createdAt, I id) {
    public static <I> Cursor<I> of(Instant createdAt, I id) {
        return new Cursor<>(createdAt, id);
    }

    public Cursor {
        Objects.requireNonNull(createdAt, "createdAt must not be null.");
        Objects.requireNonNull(id, "id must not be null.");
    }
}
