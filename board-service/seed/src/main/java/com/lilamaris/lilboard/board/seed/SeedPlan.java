package com.lilamaris.lilboard.board.seed;

import com.lilamaris.lilboard.kernel.condition.NumberPrecondition;

/**
 * 검증된 시딩 개수 계획.
 *
 * <p>총 행 수는 게시판 행과 게시글 행의 합산이다. 두 개수 모두 int 범위이고 합산은 long으로 계산하므로 int 곱은 long 범위에서 정확하며 별도의 오버플로
 * 검사가 필요하지 않다.
 */
public record SeedPlan(int boards, int postsPerBoard, SeedLimits limits) {
    public static SeedPlan of(int boards, int postsPerBoard, SeedLimits limits) {
        NumberPrecondition.requirePositive(boards);
        NumberPrecondition.requireNonNegative(postsPerBoard);
        NumberPrecondition.requireAtMost(totalRows(boards, postsPerBoard), limits.maxTotalRows());
        return new SeedPlan(boards, postsPerBoard, limits);
    }

    public long totalRows() {
        return totalRows(boards, postsPerBoard);
    }

    private static long totalRows(int boards, int postsPerBoard) {
        return (long) boards * (1L + postsPerBoard);
    }
}
