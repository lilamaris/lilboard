package com.lilamaris.lilboard.board.launcher;

import static org.assertj.core.api.Assertions.assertThat;

import com.lilamaris.lilboard.board.LauncherApplication;
import com.lilamaris.lilboard.board.domain.Board;
import com.lilamaris.lilboard.board.jpa.repository.BoardRepository;
import java.time.Instant;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.postgresql.PostgreSQLContainer;

class PostgresFlywayIntegrationTest {
    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    @DisplayName("빈 PostgreSQL에서 prod 설정으로 기동하면 Flyway V1 적용과 JPA 스키마 검증이 성공한다")
    void migratesEmptyDatabaseAndValidatesJpaSchema() throws Exception {
        try (PostgreSQLContainer postgres = startPostgres();
                ConfigurableApplicationContext context = startApplication(postgres)) {
            try (var connection = context.getBean(DataSource.class).getConnection()) {
                assertThat(connection.getMetaData().getDatabaseProductName())
                        .isEqualTo("PostgreSQL");
            }
            assertThat(context.getEnvironment().getProperty("spring.jpa.hibernate.ddl-auto"))
                    .isEqualTo("validate");
            assertThat(context.getBean(Flyway.class).info().applied())
                    .singleElement()
                    .satisfies(
                            migration ->
                                    assertThat(migration.getVersion().toString()).isEqualTo("1"));
            assertThat(context.getBean(Flyway.class).info().pending()).isEmpty();
        }
    }

    @Test
    @DisplayName("Flyway가 만든 PostgreSQL 스키마에 게시판을 저장하면 동일한 값으로 조회한다")
    void savesAndLoadsBoardOnMigratedSchema() {
        try (PostgreSQLContainer postgres = startPostgres();
                ConfigurableApplicationContext context = startApplication(postgres)) {
            BoardRepository repository = context.getBean(BoardRepository.class);
            Board board = Board.of(ID, "게시판", "설명", CREATED_AT);
            repository.saveAndFlush(board);

            assertThat(repository.findById(ID))
                    .hasValueSatisfying(
                            persisted ->
                                    assertThat(persisted)
                                            .usingRecursiveComparison()
                                            .isEqualTo(board));
        }
    }

    @Test
    @DisplayName("동일 PostgreSQL로 앱을 재기동하면 V1을 중복 적용하지 않고 기존 게시판을 유지한다")
    void restartsWithoutReapplyingMigrationAndPreservesData() {
        try (PostgreSQLContainer postgres = startPostgres()) {
            Board board = Board.of(ID, "게시판", "설명", CREATED_AT);
            try (ConfigurableApplicationContext first = startApplication(postgres)) {
                first.getBean(BoardRepository.class).saveAndFlush(board);
            }

            try (ConfigurableApplicationContext restarted = startApplication(postgres)) {
                assertThat(restarted.getBean(Flyway.class).info().applied())
                        .singleElement()
                        .satisfies(
                                migration ->
                                        assertThat(migration.getVersion().toString())
                                                .isEqualTo("1"));
                assertThat(restarted.getBean(Flyway.class).info().pending()).isEmpty();
                assertThat(restarted.getBean(BoardRepository.class).findById(ID))
                        .hasValueSatisfying(
                                persisted ->
                                        assertThat(persisted)
                                                .usingRecursiveComparison()
                                                .isEqualTo(board));
            }
        }
    }

    private static PostgreSQLContainer startPostgres() {
        PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17.6-alpine");
        postgres.start();
        return postgres;
    }

    private static ConfigurableApplicationContext startApplication(PostgreSQLContainer postgres) {
        return new SpringApplicationBuilder(LauncherApplication.class)
                .web(WebApplicationType.NONE)
                .profiles("prod")
                .run(
                        "--DB_URL=" + postgres.getJdbcUrl(),
                        "--DB_USERNAME=" + postgres.getUsername(),
                        "--DB_PASSWORD=" + postgres.getPassword());
    }
}
