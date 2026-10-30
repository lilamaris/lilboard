package com.lilamaris.lilboard.board.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lilamaris.lilboard.kernel.exception.IllegalComparisonConditionException;
import com.lilamaris.lilboard.kernel.exception.IllegalValueConditionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("SeedPlan 테스트")
class SeedPlanTest {
    private static final SeedLimits LIMITS = new SeedLimits(100_000, 500);

    @Test
    @DisplayName("게시판과 게시글 개수로 계획을 만들면 총 행은 게시판 행과 게시글 행의 합이다")
    void computesTotalRowsAsBoardRowsPlusPostRows() {
        SeedPlan plan = SeedPlan.of(10, 20, LIMITS);

        assertThat(plan.boards()).isEqualTo(10);
        assertThat(plan.postsPerBoard()).isEqualTo(20);
        assertThat(plan.totalRows()).isEqualTo(210);
    }

    @Test
    @DisplayName("게시글 개수가 0이면 게시판 행만 계획한다")
    void plansOnlyBoardRowsWhenPostsPerBoardIsZero() {
        assertThat(SeedPlan.of(5, 0, LIMITS).totalRows()).isEqualTo(5);
    }

    @Test
    @DisplayName("총 행이 상한과 정확히 같은 계획은 허용된다")
    void acceptsPlanAtUpperBound() {
        SeedPlan plan = SeedPlan.of(10, 9_999, LIMITS);

        assertThat(plan.totalRows()).isEqualTo(100_000);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -100})
    @DisplayName("게시판 개수가 0 이하이면 시딩 계획이 거부된다")
    void rejectsNonPositiveBoardCount(int boards) {
        assertThatThrownBy(() -> SeedPlan.of(boards, 10, LIMITS))
                .isInstanceOf(IllegalValueConditionException.class)
                .hasMessage("must be positive.");
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, -1_000})
    @DisplayName("게시글 개수가 음수이면 시딩 계획이 거부된다")
    void rejectsNegativePostsPerBoard(int postsPerBoard) {
        assertThatThrownBy(() -> SeedPlan.of(10, postsPerBoard, LIMITS))
                .isInstanceOf(IllegalValueConditionException.class)
                .hasMessage("must be non-negative.");
    }

    @ParameterizedTest
    @CsvSource({"100001, 0", "10, 10000", "1000, 100", "2147483647, 2147483647"})
    @DisplayName("총 행이 상한을 넘으면 시딩 계획이 거부된다")
    void rejectsPlanOverUpperBound(int boards, int postsPerBoard) {
        assertThatThrownBy(() -> SeedPlan.of(boards, postsPerBoard, LIMITS))
                .isInstanceOf(IllegalComparisonConditionException.class)
                .hasMessage("must be at most 100000.");
    }

    @Test
    @DisplayName("int 범위 최대 개수를 곱해도 long 합산이 오버플로되지 않아 상한 검사가 유지된다")
    void keepsUpperBoundCheckWhenIntCountsAreMultiplied() {
        SeedLimits small = new SeedLimits(1_000_000, 100);

        assertThatThrownBy(() -> SeedPlan.of(1_000_000, 1_000_000, small))
                .isInstanceOf(IllegalComparisonConditionException.class)
                .hasMessage("must be at most 1000000.");
        assertThat(SeedPlan.of(500, 1_999, small).totalRows()).isEqualTo(1_000_000);
    }
}
