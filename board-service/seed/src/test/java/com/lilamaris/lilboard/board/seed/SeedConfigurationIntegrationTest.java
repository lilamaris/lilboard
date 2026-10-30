package com.lilamaris.lilboard.board.seed;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles({"test", "loadtest"})
@DisplayName("loadtest 프로파일 시딩 구성 통합 테스트")
class SeedConfigurationIntegrationTest {
    @Autowired private ApplicationContext context;

    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM post");
        jdbcTemplate.update("DELETE FROM board");
    }

    @Test
    @DisplayName("loadtest 프로파일에서는 시딩 빈이 등록되고 기본 상한과 배치 크기가 적용된다")
    void registersSeedBeansWithDefaultLimits() {
        assertThat(context.getBeansOfType(SeedController.class)).hasSize(1);
        assertThat(context.getBeansOfType(SeedService.class)).hasSize(1);
        assertThat(context.getBeansOfType(SeedStore.class)).hasSize(1);

        SeedLimits limits = context.getBean(SeedLimits.class);
        assertThat(limits.maxTotalRows()).isEqualTo(100_000);
        assertThat(limits.batchSize()).isEqualTo(500);
    }

    @Test
    @DisplayName("loadtest 구성의 시딩 서비스로 게시판과 게시글을 만들면 H2에 요청한 개수만큼 행이 남는다")
    void seedsRequestedRowsThroughConfiguredService() {
        SeedResult result = context.getBean(SeedService.class).seed(2, 3);

        assertThat(result.boards()).isEqualTo(2);
        assertThat(result.posts()).isEqualTo(6);
        assertThat(count("board")).isEqualTo(2);
        assertThat(count("post")).isEqualTo(6);
        assertThat(jdbcTemplate.queryForList("SELECT board_id FROM post", UUID.class))
                .containsOnlyElementsOf(
                        jdbcTemplate.queryForList("SELECT id FROM board", UUID.class));
    }

    private int count(String table) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }
}
