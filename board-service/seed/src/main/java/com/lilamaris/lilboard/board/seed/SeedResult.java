package com.lilamaris.lilboard.board.seed;

/** 시딩 결과 개수. */
public record SeedResult(int boards, int posts, long totalRows) {
    public static SeedResult of(int boards, int posts) {
        return new SeedResult(boards, posts, (long) boards + posts);
    }
}
