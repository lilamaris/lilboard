package com.lilamaris.lilboard.board.seed;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.f4b6a3.uuid.UuidCreator;
import com.lilamaris.lilboard.board.domain.Board;
import com.lilamaris.lilboard.board.domain.Post;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SeedDataGenerator 테스트")
class SeedDataGeneratorTest {
    private static final Instant BASE = Instant.parse("2026-01-01T00:00:00Z");
    // domain Board/Post의 private 최대 길이와 같은 값 (Board 50/300, Post 300/50000)
    private static final int BOARD_NAME_MAX = 50;
    private static final int BOARD_DESCRIPTION_MAX = 300;
    private static final int POST_TITLE_MAX = 300;
    private static final int POST_CONTENT_MAX = 50000;

    private final SeedDataGenerator generator =
            new SeedDataGenerator(UuidCreator::getTimeOrderedEpoch);

    @Test
    @DisplayName("요청한 개수만큼 게시판과 게시글이 생성되고 이름·제목이 생성 순서를 따른다")
    void generatesRequestedCountsInGenerationOrder() {
        SeedDataset dataset = generator.dataset(3, 2, BASE);

        assertThat(dataset.boards())
                .extracting(Board::getName)
                .containsExactly("board-000000", "board-000001", "board-000002");
        assertThat(dataset.posts())
                .extracting(Post::getTitle)
                .containsExactly(
                        "post-000000-000000",
                        "post-000000-000001",
                        "post-000001-000000",
                        "post-000001-000001",
                        "post-000002-000000",
                        "post-000002-000001");
        assertThat(dataset.size()).isEqualTo(9);
    }

    @Test
    @DisplayName("게시글은 자기 게시판 ID를 참조하고 그 게시판보다 최근 시각에 생성된다")
    void postsReferenceTheirBoardAndAreCreatedAfterIt() {
        SeedDataset dataset = generator.dataset(2, 3, BASE);
        UUID firstBoardId = dataset.boards().get(0).getId();
        UUID secondBoardId = dataset.boards().get(1).getId();

        assertThat(dataset.posts())
                .extracting(Post::getBoardId)
                .containsExactly(
                        firstBoardId,
                        firstBoardId,
                        firstBoardId,
                        secondBoardId,
                        secondBoardId,
                        secondBoardId);
        assertThat(dataset.posts())
                .allSatisfy(
                        post ->
                                assertThat(
                                                dataset.boards().stream()
                                                        .filter(
                                                                board ->
                                                                        board.getId()
                                                                                .equals(
                                                                                        post
                                                                                                .getBoardId()))
                                                        .findFirst()
                                                        .orElseThrow()
                                                        .getCreatedAt())
                                        .isBefore(post.getCreatedAt()));
    }

    @Test
    @DisplayName("생성 순서(board → 그 게시글)에서 생성 시각가 strict 증가하고 마지막 행이 시딩 시각과 같다")
    void assignsStrictlyIncreasingTimestampsEndingAtSeedBaseTime() {
        SeedDataset dataset = generator.dataset(2, 2, BASE);

        List<Instant> generationOrder = generationOrder(dataset);

        assertThat(generationOrder).hasSize(6).doesNotHaveDuplicates();
        assertThat(generationOrder).isSorted();
        assertThat(generationOrder.getFirst()).isEqualTo(BASE.minusSeconds(5));
        assertThat(generationOrder.getLast()).isEqualTo(BASE);
    }

    @Test
    @DisplayName("게시판 생성 시각는 게시글 개수 간격(3초)으로 떨어지고 첫 게시글은 자기 게시판 직후 시각이다")
    void spacesBoardTimestampsByPostsPerBoard() {
        SeedDataset dataset = generator.dataset(3, 2, BASE);

        List<Instant> boardTimestamps = dataset.boards().stream().map(Board::getCreatedAt).toList();
        List<Instant> firstBoardPosts =
                dataset.posts().stream()
                        .filter(
                                post ->
                                        post.getBoardId()
                                                .equals(dataset.boards().getFirst().getId()))
                        .map(Post::getCreatedAt)
                        .toList();

        assertThat(Duration.between(boardTimestamps.get(0), boardTimestamps.get(1)))
                .isEqualTo(Duration.ofSeconds(3));
        assertThat(Duration.between(boardTimestamps.get(1), boardTimestamps.get(2)))
                .isEqualTo(Duration.ofSeconds(3));
        assertThat(firstBoardPosts).containsExactly(BASE.minusSeconds(7), BASE.minusSeconds(6));
    }

    @Test
    @DisplayName("게시글 개수가 0이면 게시판 행만 생성되고 생성 시각가 시딩 시각를 넘지 않는다")
    void generatesOnlyBoardsWhenPostsPerBoardIsZero() {
        SeedDataset dataset = generator.dataset(3, 0, BASE);

        assertThat(dataset.boards()).hasSize(3);
        assertThat(dataset.posts()).isEmpty();
        assertThat(dataset.boards()).extracting(Board::getCreatedAt).doesNotHaveDuplicates();
        assertThat(dataset.boards())
                .allSatisfy(board -> assertThat(board.getCreatedAt()).isBeforeOrEqualTo(BASE));
    }

    @Test
    @DisplayName("ID는 게시판과 게시글 전체에서 중복되지 않는다")
    void generatesUniqueIdsForAllRows() {
        SeedDataset dataset = generator.dataset(5, 4, BASE);

        List<UUID> ids =
                Stream.concat(
                                dataset.boards().stream().map(Board::getId),
                                dataset.posts().stream().map(Post::getId))
                        .toList();

        assertThat(ids).hasSize(25).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("상한 규모의 개수에서도 이름·설명·제목·본문이 도메인 최대 길이를 넘지 않는다")
    void keepsValuesWithinDomainLimitsAtUpperBoundScale() {
        SeedDataset dataset = generator.dataset(9_999, 10, BASE);

        assertThat(dataset.boards())
                .allSatisfy(
                        board -> {
                            assertThat(board.getName()).hasSizeLessThanOrEqualTo(BOARD_NAME_MAX);
                            assertThat(board.getDescription())
                                    .hasSizeLessThanOrEqualTo(BOARD_DESCRIPTION_MAX);
                        });
        assertThat(dataset.posts())
                .hasSize(99_990)
                .allSatisfy(
                        post -> {
                            assertThat(post.getTitle()).hasSizeLessThanOrEqualTo(POST_TITLE_MAX);
                            assertThat(post.getContent())
                                    .hasSizeLessThanOrEqualTo(POST_CONTENT_MAX);
                        });
    }

    private static List<Instant> generationOrder(SeedDataset dataset) {
        List<Instant> order = new ArrayList<>();
        for (Board board : dataset.boards()) {
            order.add(board.getCreatedAt());
            dataset.posts().stream()
                    .filter(post -> post.getBoardId().equals(board.getId()))
                    .forEach(post -> order.add(post.getCreatedAt()));
        }
        return order;
    }
}
