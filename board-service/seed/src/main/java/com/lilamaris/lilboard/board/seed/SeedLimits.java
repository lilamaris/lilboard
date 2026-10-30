package com.lilamaris.lilboard.board.seed;

import com.lilamaris.lilboard.kernel.condition.NumberPrecondition;

/**
 * 시딩 실행 조건. 한 요청에서 만들 수 있는 총 행 상한과 JDBC 배치 크기.
 *
 * <p>상한은 부하 테스트 환경의 DB에 한 요청이 과도한 데이터를 남기지 않도록 제한한다.
 */
public record SeedLimits(long maxTotalRows, int batchSize) {
    public SeedLimits {
        NumberPrecondition.requirePositive(maxTotalRows);
        NumberPrecondition.requirePositive(batchSize);
    }
}
