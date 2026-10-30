package com.lilamaris.lilboard.board.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lilamaris.lilboard.board.application.port.out.CommentFindPort;
import com.lilamaris.lilboard.board.application.port.out.CommentListPort;
import com.lilamaris.lilboard.board.application.port.out.CommentSavePort;
import com.lilamaris.lilboard.board.domain.Comment;
import com.lilamaris.lilboard.board.jpa.repository.CommentRepository;
import com.lilamaris.lilboard.kernel.pagination.Cursor;
import com.lilamaris.lilboard.kernel.pagination.CursorPageRequest;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = CommentPortJpaAdapterTest.TestApplication.class)
@Import(CommentPortJpaAdapter.class)
@DisplayName("CommentPortJpaAdapter 테스트")
class CommentPortJpaAdapterTest {
    private static final UUID COMMENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID POST_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant DELETED_AT = Instant.parse("2026-01-02T00:00:00Z");

    @Autowired private CommentSavePort savePort;

    @Autowired private CommentFindPort findPort;

    @Autowired private CommentListPort listPort;

    @Autowired private CommentRepository repository;

    @Autowired private EntityManager entityManager;

    @Test
    @DisplayName("test 프로파일로 저장할 댓글을 포트에 넘기면 H2에 같은 값으로 남고 저장 결과가 그 값이다")
    void savesCommentThroughPortAndReadsBackTheSameValues() {
        Comment comment = Comment.of(COMMENT_ID, POST_ID, "첫 번째 댓글", CREATED_AT);

        Comment saved = savePort.save(comment);
        entityManager.flush();
        entityManager.clear();

        assertThat(saved.getId()).isEqualTo(COMMENT_ID);
        assertThat(saved.getPostId()).isEqualTo(POST_ID);
        assertThat(saved.getContent()).isEqualTo("첫 번째 댓글");
        assertThat(saved.getCreatedAt()).isEqualTo(CREATED_AT);

        assertThat(repository.findById(COMMENT_ID))
                .hasValueSatisfying(
                        persisted ->
                                assertThat(persisted)
                                        .usingRecursiveComparison()
                                        .isEqualTo(comment));
    }

    @Test
    @DisplayName("test 프로파일로 저장하면 id가 이미 존재하는 댓글에서는 행을 추가하지 않고 기존 행을 갱신한다")
    void updatesExistingRowWithoutDuplicateWhenSavingWithExistingId() {
        Comment first = Comment.of(COMMENT_ID, POST_ID, "댓글", CREATED_AT);
        savePort.save(first);
        entityManager.flush();

        Comment edited = Comment.of(COMMENT_ID, POST_ID, "수정된 댓글", CREATED_AT);
        Comment saved = savePort.save(edited);
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.count()).isEqualTo(1);
        assertThat(repository.findById(COMMENT_ID))
                .hasValueSatisfying(
                        persisted ->
                                assertThat(persisted).usingRecursiveComparison().isEqualTo(edited));
        assertThat(saved.getContent()).isEqualTo("수정된 댓글");
    }

    @Test
    @DisplayName("test 프로파일로 저장할 댓글이 null이면 InvalidDataAccessApiUsageException을 던진다")
    void throwsInvalidDataAccessApiUsageExceptionWhenCommentToSaveIsNull() {
        assertThatThrownBy(() -> savePort.save(null))
                .isInstanceOf(InvalidDataAccessApiUsageException.class);
    }

    @Test
    @DisplayName("저장한 댓글을 findVisibleById로 조회하면 같은 값으로 반환한다")
    void findsSavedCommentById() {
        Comment comment = Comment.of(COMMENT_ID, POST_ID, "댓글", CREATED_AT);
        savePort.save(comment);
        entityManager.flush();
        entityManager.clear();

        assertThat(findPort.findVisibleById(COMMENT_ID))
                .hasValueSatisfying(
                        found -> assertThat(found).usingRecursiveComparison().isEqualTo(comment));
    }

    @Test
    @DisplayName("존재하지 않는 id로 조회하면 Optional.empty를 반환한다")
    void returnsEmptyWhenCommentNotFoundById() {
        assertThat(findPort.findVisibleById(UUID.randomUUID())).isEmpty();
    }

    @Test
    @DisplayName("포트로 댓글을 삭제하면 포트 조회에서 제외되지만 행과 삭제 시각은 유지한다")
    void excludesDeletedCommentFromPortFindByIdAndKeepsRow() {
        Comment comment = Comment.of(COMMENT_ID, POST_ID, "댓글", CREATED_AT);
        Comment remaining =
                Comment.of(
                        UUID.fromString("00000000-0000-0000-0000-000000000002"),
                        POST_ID,
                        "남은 댓글",
                        CREATED_AT);
        savePort.save(comment);
        savePort.save(remaining);
        entityManager.flush();

        comment.delete(DELETED_AT);
        savePort.save(comment);
        entityManager.flush();
        entityManager.clear();

        assertThat(findPort.findVisibleById(COMMENT_ID)).isEmpty();
        assertThat(findPort.findVisibleById(remaining.getId()))
                .hasValueSatisfying(
                        persisted ->
                                assertThat(persisted)
                                        .usingRecursiveComparison()
                                        .isEqualTo(remaining));
        assertThat(repository.findById(COMMENT_ID))
                .hasValueSatisfying(
                        persisted -> assertThat(persisted.getDeletedAt()).isEqualTo(DELETED_AT));
    }

    @Test
    @DisplayName("목록 조회는 삭제된 댓글을 제외하고 삭제되지 않은 댓글만 반환한다")
    void excludesDeletedCommentFromList() {
        Comment deleted = Comment.of(COMMENT_ID, POST_ID, "삭제 대상", CREATED_AT);
        Comment remaining =
                Comment.of(
                        UUID.fromString("00000000-0000-0000-0000-000000000002"),
                        POST_ID,
                        "남은 댓글",
                        CREATED_AT.plusSeconds(1));
        savePort.save(deleted);
        savePort.save(remaining);
        entityManager.flush();

        deleted.delete(DELETED_AT);
        savePort.save(deleted);
        entityManager.flush();
        entityManager.clear();

        var page = listPort.findVisibleByPostId(POST_ID, CursorPageRequest.of(null, 2));

        assertThat(page.items()).extracting(Comment::getContent).containsExactly("남은 댓글");
        assertThat(page.hasNext()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    @DisplayName("모든 댓글이 삭제되면 첫 페이지와 범위 밖 페이지 모두 빈 목록을 반환한다")
    void returnsEmptyPageWhenAllCommentsAreDeleted(int pageNumber) {
        Comment deleted = Comment.of(COMMENT_ID, POST_ID, "삭제된 댓글", CREATED_AT);
        deleted.delete(DELETED_AT);
        savePort.save(deleted);
        entityManager.flush();
        entityManager.clear();

        var page = listPort.findVisibleByPostId(POST_ID, CursorPageRequest.of(null, 1));

        assertThat(page.items()).isEmpty();
        assertThat(page.hasNext()).isFalse();
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("커서 없이 첫 페이지를 조회하면 생성 시각·ID 내림차순으로 정렬해 반환하고 다음 커서를 만든다")
    void findsFirstPageWithCursor() {
        saveCommentFixtures();

        var page = listPort.findVisibleByPostId(POST_ID, CursorPageRequest.of(null, 2));

        assertThat(page.items()).extracting(Comment::getContent).containsExactly("최신", "이전");
        assertThat(page.items()).hasSize(2);
        assertThat(page.nextCursor()).isNotNull();
        assertThat(page.nextCursor().createdAt()).isEqualTo(CREATED_AT);
        assertThat(page.nextCursor().id())
                .isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000003"));
        assertThat(page.hasNext()).isTrue();
    }

    @Test
    @DisplayName("다음 커서를 주면 해당 커서 이후의 댓글 페이지를 반환한다")
    void findsNextPageByCursor() {
        saveCommentFixtures();

        var firstPage = listPort.findVisibleByPostId(POST_ID, CursorPageRequest.of(null, 2));
        Cursor<UUID> nextCursor = firstPage.nextCursor();

        var secondPage = listPort.findVisibleByPostId(POST_ID, CursorPageRequest.of(nextCursor, 2));

        assertThat(secondPage.items()).extracting(Comment::getContent).containsExactly("동시 생성");
        assertThat(secondPage.items()).hasSize(1);
        assertThat(secondPage.nextCursor()).isNull();
        assertThat(secondPage.hasNext()).isFalse();
    }

    @Test
    @DisplayName("댓글이 없으면 빈 목록을 반환한다")
    void findsEmptyPage() {
        var page = listPort.findVisibleByPostId(POST_ID, CursorPageRequest.of(null, 2));

        assertThat(page.items()).isEmpty();
        assertThat(page.nextCursor()).isNull();
        assertThat(page.hasNext()).isFalse();
    }

    private void saveCommentFixtures() {
        savePort.save(
                Comment.of(
                        UUID.fromString("00000000-0000-0000-0000-000000000003"),
                        POST_ID,
                        "이전",
                        CREATED_AT));
        savePort.save(
                Comment.of(
                        UUID.fromString("00000000-0000-0000-0000-000000000002"),
                        POST_ID,
                        "동시 생성",
                        CREATED_AT));
        savePort.save(
                Comment.of(
                        UUID.fromString("00000000-0000-0000-0000-000000000004"),
                        POST_ID,
                        "최신",
                        CREATED_AT.plusSeconds(2)));
        entityManager.flush();
        entityManager.clear();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = Comment.class)
    @EnableJpaRepositories(basePackageClasses = CommentRepository.class)
    static class TestApplication {}
}
