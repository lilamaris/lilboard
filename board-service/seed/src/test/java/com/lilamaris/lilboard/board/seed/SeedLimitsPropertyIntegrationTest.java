package com.lilamaris.lilboard.board.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lilamaris.lilboard.kernel.exception.IllegalComparisonConditionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {"board.seed.max-total-rows=10", "board.seed.batch-size=3"})
@ActiveProfiles({"test", "loadtest"})
@DisplayName("시딩 상한·배치 설정 통합 테스트")
class SeedLimitsPropertyIntegrationTest {
    @Autowired private SeedService seedService;

    @Autowired private SeedLimits limits;

    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM post");
        jdbcTemplate.update("DELETE FROM board");
    }

    @Test
    @DisplayName("총 행 상한을 설정으로 낮추면 상한 안의 요청만 시딩하고 초과 요청은 저장하지 않는다")
    void appliesConfiguredUpperBoundAndRejectsRequestsOverIt() {
        assertThat(limits.maxTotalRows()).isEqualTo(10);

        SeedResult accepted = seedService.seed(2, 3);

        assertThat(accepted.boards()).isEqualTo(2);
        assertThat(accepted.posts()).isEqualTo(6);
        assertThat(count("board")).isEqualTo(2);
        assertThat(count("post")).isEqualTo(6);

        assertThatThrownBy(() -> seedService.seed(5, 6))
                .isInstanceOf(IllegalComparisonConditionException.class)
                .hasMessage("must be at most 10.");
        assertThat(count("board")).isEqualTo(2);
        assertThat(count("post")).isEqualTo(6);
    }

    @Test
    @DisplayName("배치 크기를 3으로 줄여도 상한 안의 요청한 모든 행이 저장된다")
    void savesAllRowsWithSmallBatchSize() {
        SeedResult result = seedService.seed(2, 4);

        assertThat(result.totalRows()).isEqualTo(10);
        assertThat(count("board")).isEqualTo(2);
        assertThat(count("post")).isEqualTo(8);
    }

    private int count(String table) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }
}
