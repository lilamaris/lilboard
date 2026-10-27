package com.lilamaris.lilboard.board.launcher;

import static org.assertj.core.api.Assertions.assertThat;

import com.lilamaris.lilboard.board.LauncherApplication;
import com.lilamaris.lilboard.board.application.port.in.CreateBoardUseCase;
import com.lilamaris.lilboard.board.application.port.in.DeleteBoardUseCase;
import com.lilamaris.lilboard.board.application.port.in.command.CreateBoardCommand;
import com.lilamaris.lilboard.board.application.port.in.command.DeleteBoardCommand;
import com.lilamaris.lilboard.board.application.port.out.BoardFindPort;
import com.lilamaris.lilboard.board.domain.Board;
import com.lilamaris.lilboard.board.domain.Post;
import com.lilamaris.lilboard.board.jpa.repository.BoardRepository;
import com.lilamaris.lilboard.board.jpa.repository.PostRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
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

class PostgresFlywayIntegrationTest {
    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    @DisplayName("빈 PostgreSQL에서 prod 설정으로 기동하면 최신 마이그레이션 적용과 JPA 스키마 검증이 성공한다")
    void migratesEmptyDatabaseAndValidatesJpaSchema() throws Exception {
        try (PostgreSQLContainer postgres = startPostgres();
                ConfigurableApplicationContext context = startApplication(postgres)) {
            try (var connection = context.getBean(DataSource.class).getConnection()) {
                assertThat(connection.getMetaData().getDatabaseProductName())
                        .isEqualTo("PostgreSQL");
            }
            assertThat(context.getEnvironment().getProperty("spring.jpa.hibernate.ddl-auto"))
                    .isEqualTo("validate");
            assertThat(context.getBean(Flyway.class).info().applied()).isNotEmpty();
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
    @DisplayName("게시글이 있는 게시판을 삭제하면 PostgreSQL 외래 키를 유지하고 게시판 행과 게시글을 보존한다")
    void softDeletesBoardWithPostsWithoutViolatingForeignKey() {
        try (PostgreSQLContainer postgres = startPostgres();
                ConfigurableApplicationContext context = startApplication(postgres)) {
            BoardRepository boards = context.getBean(BoardRepository.class);
            PostRepository posts = context.getBean(PostRepository.class);
            Board board = Board.of(ID, "게시판", "설명", CREATED_AT);
            Instant updatedAt = CREATED_AT.plusSeconds(1);
            board.updateName("수정한 게시판", updatedAt);
            boards.saveAndFlush(board);
            Post post =
                    Post.of(
                            UUID.fromString("00000000-0000-0000-0000-000000000002"),
                            ID,
                            "제목",
                            "본문",
                            CREATED_AT);
            posts.saveAndFlush(post);

            context.getBean(DeleteBoardUseCase.class).delete(new DeleteBoardCommand(ID));

            assertThat(context.getBean(BoardFindPort.class).findById(ID)).isEmpty();
            assertThat(boards.findById(ID))
                    .hasValueSatisfying(
                            persisted -> {
                                assertThat(persisted.getDeletedAt()).isAfterOrEqualTo(CREATED_AT);
                                assertThat(persisted)
                                        .usingRecursiveComparison()
                                        .ignoringFields("deletedAt")
                                        .isEqualTo(board);
                            });
            assertThat(posts.findById(post.getId()))
                    .hasValueSatisfying(
                            persisted ->
                                    assertThat(persisted)
                                            .usingRecursiveComparison()
                                            .isEqualTo(post));
        }
    }

    @Test
    @DisplayName("동일 PostgreSQL로 앱을 재기동하면 마이그레이션 이력과 기존 게시판을 유지한다")
    void restartsWithoutReapplyingMigrationAndPreservesData() {
        try (PostgreSQLContainer postgres = startPostgres()) {
            Board board = Board.of(ID, "게시판", "설명", CREATED_AT);
            List<Map<String, Object>> appliedMigrations;
            try (ConfigurableApplicationContext first = startApplication(postgres)) {
                first.getBean(BoardRepository.class).saveAndFlush(board);
                appliedMigrations = migrationHistory(first);
                assertThat(appliedMigrations).isNotEmpty();
            }

            try (ConfigurableApplicationContext restarted = startApplication(postgres)) {
                assertThat(migrationHistory(restarted))
                        .containsExactlyElementsOf(appliedMigrations);
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

    @Test
    @DisplayName("PostgreSQL의 기존 UUIDv4 게시판과 유스케이스가 생성한 UUIDv7 게시판을 재기동 후에도 조회한다")
    void preservesLegacyAndGeneratedVersionSevenBoardsAfterRestart() {
        try (PostgreSQLContainer postgres = startPostgres()) {
            UUID legacyId = UUID.fromString("c9bf9a5c-7a33-4b0e-9c8a-1f2e3d4c5b6a");
            Board legacy = Board.of(legacyId, "기존 게시판", "설명", CREATED_AT);
            UUID newId;
            try (ConfigurableApplicationContext first = startApplication(postgres)) {
                first.getBean(BoardRepository.class).saveAndFlush(legacy);
                var result =
                        first.getBean(CreateBoardUseCase.class)
                                .create(new CreateBoardCommand("신규 게시판", "설명"));
                newId = result.id();
                assertThat(newId.version()).isEqualTo(7);
            }
            try (ConfigurableApplicationContext restarted = startApplication(postgres)) {
                BoardRepository repository = restarted.getBean(BoardRepository.class);
                assertThat(repository.findById(legacyId))
                        .hasValueSatisfying(
                                actual ->
                                        assertThat(actual)
                                                .usingRecursiveComparison()
                                                .isEqualTo(legacy));
                assertThat(repository.findById(newId))
                        .hasValueSatisfying(
                                actual -> {
                                    assertThat(actual.getId()).isEqualTo(newId);
                                    assertThat(actual.getName()).isEqualTo("신규 게시판");
                                    assertThat(actual.getDescription()).isEqualTo("설명");
                                });
            }
        }
    }

    private static List<Map<String, Object>> migrationHistory(
            ConfigurableApplicationContext context) {
        return new JdbcTemplate(context.getBean(DataSource.class))
                .queryForList("SELECT * FROM flyway_schema_history ORDER BY installed_rank");
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
