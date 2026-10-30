package com.lilamaris.lilboard.board.seed;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.f4b6a3.uuid.UuidCreator;
import com.lilamaris.lilboard.board.domain.Board;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@DisplayName("JdbcSeedStore H2 통합 테스트")
class JdbcSeedStoreIntegrationTest {
    private static final Instant BASE = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired private JdbcTemplate jdbcTemplate;

    private final SeedDataGenerator generator =
            new SeedDataGenerator(UuidCreator::getTimeOrderedEpoch);

    private SeedStore store;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM post");
        jdbcTemplate.update("DELETE FROM board");
        store = new JdbcSeedStore(jdbcTemplate, new SeedLimits(100_000, 2));
    }

    @Test
    @DisplayName("H2에 배치로 저장하면 게시판·게시글 행 수와 컬럼 값이 생성된 값 그대로 기록된다")
    void savesRowsAndPersistsGeneratedValues() {
        SeedDataset dataset = generator.dataset(2, 3, BASE);

        store.save(dataset.boards(), dataset.posts());

        Board first = dataset.boards().getFirst();
        Map<String, Object> row =
                jdbcTemplate.queryForMap("SELECT * FROM board WHERE id = ?", first.getId());
        assertThat(count("board")).isEqualTo(2);
        assertThat(count("post")).isEqualTo(6);
        assertThat(row)
                .containsEntry("NAME", first.getName())
                .containsEntry("DESCRIPTION", first.getDescription())
                .containsEntry("UPDATED_AT", null)
                .containsEntry("DELETED_AT", null);
        assertThat(((OffsetDateTime) row.get("CREATED_AT")).toInstant())
                .isEqualTo(first.getCreatedAt());
    }

    @Test
    @DisplayName("배치 크기가 행 수보다 작아도 모든 행이 저장되고 게시글은 게시판 외래 키를 만족한다")
    void savesAllRowsWhenBatchSizeIsSmallerThanRowCount() {
        SeedDataset dataset = generator.dataset(5, 3, BASE);

        store.save(dataset.boards(), dataset.posts());

        List<UUID> postBoardIds =
                jdbcTemplate.queryForList(
                        "SELECT board_id FROM post ORDER BY created_at ASC", UUID.class);
        assertThat(count("board")).isEqualTo(5);
        assertThat(count("post")).isEqualTo(15);
        assertThat(postBoardIds)
                .containsExactlyElementsOf(
                        dataset.posts().stream()
                                .map(com.lilamaris.lilboard.board.domain.Post::getBoardId)
                                .toList());
    }

    @Test
    @DisplayName("게시판 생성 시각는 게시글 개수 간격(3초)으로 내림차순이고 마지막 게시글이 시딩 시각이다")
    void storesBoardTimestampsSpacedByPostsPerBoardInDescendingOrder() {
        SeedDataset dataset = generator.dataset(3, 2, BASE);

        store.save(dataset.boards(), dataset.posts());

        List<Timestamp> boards =
                jdbcTemplate.queryForList(
                        "SELECT created_at FROM board ORDER BY created_at DESC", Timestamp.class);
        Timestamp latestPost =
                jdbcTemplate.queryForObject("SELECT MAX(created_at) FROM post", Timestamp.class);

        assertThat(boards).hasSize(3);
        assertThat(boards.getFirst().toInstant()).isEqualTo(BASE.minusSeconds(2));
        assertThat(Duration.between(boards.get(1).toInstant(), boards.getFirst().toInstant()))
                .isEqualTo(Duration.ofSeconds(3));
        assertThat(Duration.between(boards.get(2).toInstant(), boards.get(1).toInstant()))
                .isEqualTo(Duration.ofSeconds(3));
        assertThat(latestPost.toInstant()).isEqualTo(BASE);
    }

    private int count(String table) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }
}
