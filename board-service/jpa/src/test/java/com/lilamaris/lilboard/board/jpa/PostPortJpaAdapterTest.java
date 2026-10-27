package com.lilamaris.lilboard.board.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import com.lilamaris.lilboard.board.application.port.out.PostFindPort;
import com.lilamaris.lilboard.board.application.port.out.PostListPort;
import com.lilamaris.lilboard.board.application.port.out.PostSavePort;
import com.lilamaris.lilboard.board.domain.Board;
import com.lilamaris.lilboard.board.domain.Post;
import com.lilamaris.lilboard.board.jpa.repository.BoardRepository;
import com.lilamaris.lilboard.board.jpa.repository.PostRepository;
import com.lilamaris.lilboard.kernel.pagination.OffsetPageRequest;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = PostPortJpaAdapterTest.TestApplication.class)
@Import(PostPortJpaAdapter.class)
class PostPortJpaAdapterTest {
    private static final UUID POST_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID BOARD_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant DELETED_AT = Instant.parse("2026-01-02T00:00:00Z");

    @Autowired private PostSavePort savePort;

    @Autowired private PostFindPort findPort;

    @Autowired private PostListPort listPort;

    @Autowired private PostRepository repository;

    @Autowired private BoardRepository boardRepository;

    @Autowired private EntityManager entityManager;

    @Test
    @DisplayName("가시 게시글을 ID로 조회하면 저장한 게시글 값을 그대로 반환한다")
    void findsVisibleSavedPostById() {
        Post post = savePost(saveBoard());
        entityManager.flush();
        entityManager.clear();

        assertThat(findPort.findVisibleById(POST_ID))
                .hasValueSatisfying(
                        found -> assertThat(found).usingRecursiveComparison().isEqualTo(post));
    }

    @Test
    @DisplayName("소속 게시판이 삭제된 게시글은 가시 조회에서 제외되고 게시글·게시판 행과 삭제 시각은 유지한다")
    void excludesPostOfDeletedBoardFromVisibleFindAndKeepsRows() {
        Board board = saveBoard();
        Post post = savePost(board);
        entityManager.flush();

        board.delete(DELETED_AT);
        boardRepository.save(board);
        entityManager.flush();
        entityManager.clear();

        assertThat(findPort.findVisibleById(POST_ID)).isEmpty();
        assertThat(repository.findById(POST_ID))
                .hasValueSatisfying(
                        persisted ->
                                assertThat(persisted).usingRecursiveComparison().isEqualTo(post));
        assertThat(boardRepository.findById(BOARD_ID))
                .hasValueSatisfying(
                        persisted -> assertThat(persisted.getDeletedAt()).isEqualTo(DELETED_AT));
    }

    @Test
    @DisplayName("소속 게시판 행이 없으면 가시 게시글로 조회하지 못해 빈 결과를 반환한다")
    void returnsEmptyWhenOwningBoardRowIsMissing() {
        savePort.save(Post.of(POST_ID, BOARD_ID, "기존 제목", "기존 본문", CREATED_AT));
        entityManager.flush();
        entityManager.clear();

        assertThat(findPort.findVisibleById(POST_ID)).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 게시글 ID로 가시 조회하면 빈 결과를 반환한다")
    void returnsEmptyWhenPostNotFound() {
        assertThat(findPort.findVisibleById(POST_ID)).isEmpty();
    }

    @Test
    @DisplayName("조회한 게시글을 수정해 저장하면 기존 행의 제목·본문·수정 시각을 갱신한다")
    void persistsUpdatedPostWithoutAddingRow() {
        savePost(saveBoard());
        entityManager.flush();
        entityManager.clear();
        Post post = findPort.findVisibleById(POST_ID).orElseThrow();
        Instant updatedAt = CREATED_AT.plusSeconds(1);

        post.updateTitle("새 제목", updatedAt);
        post.updateContent("첫 번째 줄\n두 번째 줄", updatedAt);
        savePort.save(post);
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.count()).isEqualTo(1);
        assertThat(findPort.findVisibleById(POST_ID))
                .hasValueSatisfying(
                        reread -> {
                            assertThat(reread.getId()).isEqualTo(POST_ID);
                            assertThat(reread.getBoardId()).isEqualTo(BOARD_ID);
                            assertThat(reread.getTitle()).isEqualTo("새 제목");
                            assertThat(reread.getContent()).isEqualTo("첫 번째 줄\n두 번째 줄");
                            assertThat(reread.getCreatedAt()).isEqualTo(CREATED_AT);
                            assertThat(reread.getUpdatedAt()).isEqualTo(updatedAt);
                        });
    }

    @Test
    @DisplayName("목록 조회는 삭제된 게시판의 게시글을 제외하고 전체 개수에 포함하지 않는다")
    void excludesPostOfDeletedBoardFromListAndTotalElements() {
        UUID deletedBoardId = UUID.fromString("00000000-0000-0000-0000-000000000003");
        UUID deletedPostId = UUID.fromString("00000000-0000-0000-0000-000000000004");
        Board deletedBoard = Board.of(deletedBoardId, "삭제 대상 게시판", "설명", CREATED_AT);
        deletedBoard.delete(DELETED_AT);
        boardRepository.save(deletedBoard);
        boardRepository.save(Board.of(BOARD_ID, "남은 게시판", "설명", CREATED_AT));
        savePort.save(Post.of(deletedPostId, deletedBoardId, "삭제된 게시글", "본문", CREATED_AT));
        savePort.save(Post.of(POST_ID, BOARD_ID, "남은 게시글", "본문", CREATED_AT));
        entityManager.flush();
        entityManager.clear();

        var page = listPort.findAllVisible(OffsetPageRequest.of(0, 2));

        assertThat(page.items()).extracting(Post::getTitle).containsExactly("남은 게시글");
        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(repository.count()).isEqualTo(2);
    }

    @ParameterizedTest
    @CsvSource({"0,최신,true", "1,이전,false"})
    @DisplayName("삭제된 게시판의 게시글이 정렬 순서 사이에 있어도 페이지 오프셋과 전체 개수는 남은 게시글만 기준으로 계산한다")
    void paginatesOnlyVisiblePosts(int pageNumber, String expectedTitle, boolean hasNext) {
        saveVisibleFixtures();
        UUID deletedBoardId = UUID.fromString("00000000-0000-0000-0000-000000000003");
        UUID deletedPostId = UUID.fromString("00000000-0000-0000-0000-000000000004");
        Board deletedBoard = Board.of(deletedBoardId, "삭제 대상 게시판", "설명", CREATED_AT);
        deletedBoard.delete(DELETED_AT);
        boardRepository.save(deletedBoard);
        savePort.save(
                Post.of(
                        deletedPostId,
                        deletedBoardId,
                        "삭제된 중간 게시글",
                        "본문",
                        CREATED_AT.plusSeconds(1)));
        entityManager.flush();
        entityManager.clear();

        var page = listPort.findAllVisible(OffsetPageRequest.of(pageNumber, 1));

        assertThat(page.items()).extracting(Post::getTitle).containsExactly(expectedTitle);
        assertThat(page.page()).isEqualTo(pageNumber);
        assertThat(page.size()).isEqualTo(1);
        assertThat(page.totalElements()).isEqualTo(2);
        assertThat(page.totalPages()).isEqualTo(2);
        assertThat(page.hasNext()).isEqualTo(hasNext);
        assertThat(page.hasPrevious()).isEqualTo(pageNumber > 0);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    @DisplayName("모든 게시판이 삭제되면 첫 페이지와 범위 밖 페이지 모두 빈 목록과 전체 개수 0을 반환한다")
    void returnsEmptyPageWhenAllBoardsAreDeleted(int pageNumber) {
        Board deletedBoard = Board.of(BOARD_ID, "삭제된 게시판", "설명", CREATED_AT);
        deletedBoard.delete(DELETED_AT);
        boardRepository.save(deletedBoard);
        savePort.save(Post.of(POST_ID, BOARD_ID, "게시글", "본문", CREATED_AT));
        entityManager.flush();
        entityManager.clear();

        var page = listPort.findAllVisible(OffsetPageRequest.of(pageNumber, 1));

        assertThat(page.items()).isEmpty();
        assertThat(page.page()).isEqualTo(pageNumber);
        assertThat(page.size()).isEqualTo(1);
        assertThat(page.totalElements()).isZero();
        assertThat(page.totalPages()).isZero();
        assertThat(page.hasNext()).isFalse();
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("소속 게시판 행이 없는 게시글은 목록과 전체 개수에서 제외한다")
    void excludesPostWithoutBoardRowFromList() {
        savePort.save(Post.of(POST_ID, BOARD_ID, "고아 게시글", "본문", CREATED_AT));
        entityManager.flush();
        entityManager.clear();

        var page = listPort.findAllVisible(OffsetPageRequest.of(0, 2));

        assertThat(page.items()).isEmpty();
        assertThat(page.totalElements()).isZero();
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("목록 첫 페이지는 생성 시각과 ID 내림차순으로 조회하고 전체 개수를 반환한다")
    void findsFirstPageInStableOrder() {
        savePageFixtures();

        var page = listPort.findAllVisible(OffsetPageRequest.of(0, 2));

        assertThat(page.items()).extracting(Post::getTitle).containsExactly("동시 생성", "최신");
        assertThat(page.page()).isZero();
        assertThat(page.size()).isEqualTo(2);
        assertThat(page.totalElements()).isEqualTo(3);
        assertThat(page.totalPages()).isEqualTo(2);
        assertThat(page.hasPrevious()).isFalse();
        assertThat(page.hasNext()).isTrue();
    }

    @Test
    @DisplayName("마지막 페이지는 오프셋 이후 남은 게시글과 전체 개수를 반환한다")
    void findsLastPage() {
        savePageFixtures();

        var page = listPort.findAllVisible(OffsetPageRequest.of(1, 2));

        assertThat(page.items()).extracting(Post::getTitle).containsExactly("이전");
        assertThat(page.page()).isEqualTo(1);
        assertThat(page.size()).isEqualTo(2);
        assertThat(page.totalElements()).isEqualTo(3);
        assertThat(page.hasPrevious()).isTrue();
        assertThat(page.hasNext()).isFalse();
    }

    @Test
    @DisplayName("게시글이 없으면 빈 목록과 전체 개수 0을 반환한다")
    void findsEmptyPage() {
        var page = listPort.findAllVisible(OffsetPageRequest.of(0, 2));

        assertThat(page.items()).isEmpty();
        assertThat(page.page()).isZero();
        assertThat(page.size()).isEqualTo(2);
        assertThat(page.totalElements()).isZero();
        assertThat(page.totalPages()).isZero();
        assertThat(page.hasPrevious()).isFalse();
        assertThat(page.hasNext()).isFalse();
    }

    @Test
    @DisplayName("마지막 페이지를 벗어나면 빈 목록과 실제 전체 개수를 반환한다")
    void findsPageBeyondLast() {
        savePageFixtures();

        var page = listPort.findAllVisible(OffsetPageRequest.of(2, 2));

        assertThat(page.items()).isEmpty();
        assertThat(page.page()).isEqualTo(2);
        assertThat(page.size()).isEqualTo(2);
        assertThat(page.totalElements()).isEqualTo(3);
        assertThat(page.hasPrevious()).isTrue();
        assertThat(page.hasNext()).isFalse();
    }

    private void saveVisibleFixtures() {
        boardRepository.save(Board.of(BOARD_ID, "남은 게시판", "설명", CREATED_AT));
        savePort.save(
                Post.of(
                        UUID.fromString("00000000-0000-0000-0000-000000000005"),
                        BOARD_ID,
                        "최신",
                        "본문",
                        CREATED_AT.plusSeconds(2)));
        savePort.save(Post.of(POST_ID, BOARD_ID, "이전", "본문", CREATED_AT));
        entityManager.flush();
        entityManager.clear();
    }

    private void savePageFixtures() {
        boardRepository.save(Board.of(BOARD_ID, "게시판", "설명", CREATED_AT));
        savePort.save(
                Post.of(
                        UUID.fromString("00000000-0000-0000-0000-000000000003"),
                        BOARD_ID,
                        "이전",
                        "본문",
                        CREATED_AT));
        savePort.save(Post.of(POST_ID, BOARD_ID, "최신", "본문", CREATED_AT.plusSeconds(1)));
        savePort.save(
                Post.of(
                        UUID.fromString("00000000-0000-0000-0000-000000000002"),
                        BOARD_ID,
                        "동시 생성",
                        "본문",
                        CREATED_AT.plusSeconds(1)));
        entityManager.flush();
        entityManager.clear();
    }

    private Board saveBoard() {
        return boardRepository.save(Board.of(BOARD_ID, "게시판", "설명", CREATED_AT));
    }

    private Post savePost(Board board) {
        Post post = Post.of(POST_ID, board.getId(), "기존 제목", "기존 본문", CREATED_AT);
        return savePort.save(post);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = Post.class)
    @EnableJpaRepositories(basePackageClasses = PostRepository.class)
    static class TestApplication {}
}
