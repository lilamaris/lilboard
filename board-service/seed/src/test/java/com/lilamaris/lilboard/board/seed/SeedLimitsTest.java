package com.lilamaris.lilboard.board.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lilamaris.lilboard.kernel.exception.IllegalValueConditionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("SeedLimits 테스트")
class SeedLimitsTest {
    @Test
    @DisplayName("양수 상한과 배치 크기를 설정하면 값이 그대로 유지된다")
    void keepsPositiveUpperBoundAndBatchSize() {
        SeedLimits limits = new SeedLimits(100_000, 500);

        assertThat(limits.maxTotalRows()).isEqualTo(100_000);
        assertThat(limits.batchSize()).isEqualTo(500);
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1, -100_000})
    @DisplayName("총 행 상한이 0 이하이면 시딩 조건이 거부된다")
    void rejectsNonPositiveUpperBound(long maxTotalRows) {
        assertThatThrownBy(() -> new SeedLimits(maxTotalRows, 500))
                .isInstanceOf(IllegalValueConditionException.class)
                .hasMessage("must be positive.");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -500})
    @DisplayName("배치 크기가 0 이하이면 시딩 조건이 거부된다")
    void rejectsNonPositiveBatchSize(int batchSize) {
        assertThatThrownBy(() -> new SeedLimits(100_000, batchSize))
                .isInstanceOf(IllegalValueConditionException.class)
                .hasMessage("must be positive.");
    }
}
