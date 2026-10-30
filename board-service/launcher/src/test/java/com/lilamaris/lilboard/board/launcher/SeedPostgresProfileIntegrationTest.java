package com.lilamaris.lilboard.board.launcher;

import static org.assertj.core.api.Assertions.assertThat;

import com.lilamaris.lilboard.board.LauncherApplication;
import com.lilamaris.lilboard.board.seed.SeedController;
import com.lilamaris.lilboard.board.seed.SeedResult;
import com.lilamaris.lilboard.board.seed.SeedService;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;

@DisplayName("PostgreSQL에서의 시딩 프로파일 통합 테스트")
class SeedPostgresProfileIntegrationTest {
    @Test
    @DisplayName("prod 프로파일만 사용하는 PostgreSQL 환경에서는 시딩 빈이 등록되지 않는다")
    void hasNoSeedBeansInProdProfile() {
        try (PostgreSQLContainer postgres = startPostgres();
                ConfigurableApplicationContext context = startApplication(postgres, "prod")) {
            assertThat(context.getBeansOfType(SeedController.class)).isEmpty();
            assertThat(context.getBeansOfType(SeedService.class)).isEmpty();
            assertThat(context.getBean(Flyway.class).info().pending()).isEmpty();
        }
    }

    @Test
    @DisplayName("prod에 loadtest를 추가하면 Flyway 스키마에 외래 키를 유지한 채 시딩 행이 저장된다")
    void seedsRowsOnFlywaySchemaKeepingForeignKeyWhenLoadtestIsAdded() {
        try (PostgreSQLContainer postgres = startPostgres();
                ConfigurableApplicationContext context =
                        startApplication(postgres, "prod", "loadtest")) {
            JdbcTemplate jdbc = new JdbcTemplate(context.getBean(DataSource.class));

            SeedResult result = context.getBean(SeedService.class).seed(2, 3);

            assertThat(result.boards()).isEqualTo(2);
            assertThat(result.posts()).isEqualTo(6);
            assertThat(count(jdbc, "board")).isEqualTo(2);
            assertThat(count(jdbc, "post")).isEqualTo(6);
            assertThat(
                            jdbc.queryForList(
                                    "SELECT p.board_id FROM post p LEFT JOIN board b ON p.board_id = b.id"
                                            + " WHERE b.id IS NULL",
                                    UUID.class))
                    .isEmpty();
        }
    }

    @Test
    @DisplayName("Flyway 스키마의 created_at에 시딩한 시각이 초 단위 간격으로 timestamptz로 저장된다")
    void storesSeededTimestampsAsTimestamptzWithOneSecondSpacing() {
        try (PostgreSQLContainer postgres = startPostgres();
                ConfigurableApplicationContext context =
                        startApplication(postgres, "prod", "loadtest")) {
            JdbcTemplate jdbc = new JdbcTemplate(context.getBean(DataSource.class));

            context.getBean(SeedService.class).seed(3, 1);

            String columnType =
                    jdbc.queryForObject(
                            "SELECT data_type FROM information_schema.columns"
                                    + " WHERE table_name = 'board' AND column_name = 'created_at'",
                            String.class);
            List<Instant> boardTimes =
                    jdbc
                            .queryForList(
                                    "SELECT created_at FROM board ORDER BY created_at DESC",
                                    Timestamp.class)
                            .stream()
                            .map(Timestamp::toInstant)
                            .toList();
            Instant latestPost =
                    jdbc.queryForObject("SELECT MAX(created_at) FROM post", Timestamp.class)
                            .toInstant();
            Instant latestBoard =
                    jdbc.queryForObject("SELECT MAX(created_at) FROM board", Timestamp.class)
                            .toInstant();

            assertThat(columnType).isEqualTo("timestamp with time zone");
            assertThat(boardTimes).hasSize(3);
            assertThat(Duration.between(boardTimes.get(1), boardTimes.getFirst()))
                    .isEqualTo(Duration.ofSeconds(2));
            assertThat(Duration.between(boardTimes.get(2), boardTimes.get(1)))
                    .isEqualTo(Duration.ofSeconds(2));
            assertThat(latestPost).isAfter(latestBoard);
        }
    }

    @Test
    @DisplayName("시딩한 게시판은 삭제되지 않은 게시판 행으로 남고 이름이 생성 순서를 따른다")
    void keepsSeededBoardsAsNonDeletedRowsInGenerationOrder() {
        try (PostgreSQLContainer postgres = startPostgres();
                ConfigurableApplicationContext context =
                        startApplication(postgres, "prod", "loadtest")) {
            JdbcTemplate jdbc = new JdbcTemplate(context.getBean(DataSource.class));

            context.getBean(SeedService.class).seed(2, 1);

            List<String> names =
                    jdbc.queryForList(
                            "SELECT name FROM board WHERE deleted_at IS NULL ORDER BY created_at ASC",
                            String.class);

            assertThat(names).containsExactly("board-000000", "board-000001");
            assertThat(
                            jdbc.queryForList(
                                    "SELECT id FROM board WHERE deleted_at IS NOT NULL",
                                    UUID.class))
                    .isEmpty();
        }
    }

    private static int count(JdbcTemplate jdbc, String table) {
        Number count = jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Number.class);
        return count.intValue();
    }

    private static PostgreSQLContainer startPostgres() {
        PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17.6-alpine");
        postgres.start();
        return postgres;
    }

    private static ConfigurableApplicationContext startApplication(
            PostgreSQLContainer postgres, String... profiles) {
        return new SpringApplicationBuilder(LauncherApplication.class)
                .web(WebApplicationType.NONE)
                .profiles(profiles)
                .run(
                        "--DB_URL=" + postgres.getJdbcUrl(),
                        "--DB_USERNAME=" + postgres.getUsername(),
                        "--DB_PASSWORD=" + postgres.getPassword());
    }
}
