package com.lilamaris.lilboard.board.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lilamaris.lilboard.board.application.port.out.BoardFindPort;
import com.lilamaris.lilboard.board.application.port.out.BoardListPort;
import com.lilamaris.lilboard.board.application.port.out.BoardSavePort;
import com.lilamaris.lilboard.board.domain.Board;
import com.lilamaris.lilboard.board.jpa.repository.BoardRepository;
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
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = BoardPortJpaAdapterTest.TestApplication.class)
@Import(BoardPortJpaAdapter.class)
@DisplayName("BoardPortJpaAdapter 테스트")
class BoardPortJpaAdapterTest {
    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant DELETED_AT = Instant.parse("2026-01-02T00:00:00Z");

    @Autowired private BoardSavePort savePort;

    @Autowired private BoardFindPort findPort;

    @Autowired private BoardListPort listPort;

    @Autowired private BoardRepository repository;

    @Autowired private EntityManager entityManager;

    @Test
    @DisplayName("test 프로파일로 저장할 게시물을 포트에 넘기면 H2에 같은 값으로 남고 저장 결과가 그 값이다")
    void savesBoardThroughPortAndReadsBackTheSameValues() {
        Board board = Board.of(ID, "자유게시판", "자유롭게 이야기를 나눠요", CREATED_AT);

        Board saved = savePort.save(board);
        entityManager.flush();
        entityManager.clear();

        assertThat(saved.getId()).isEqualTo(ID);
        assertThat(saved.getName()).isEqualTo("자유게시판");
        assertThat(saved.getDescription()).isEqualTo("자유롭게 이야기를 나눠요");
        assertThat(saved.getCreatedAt()).isEqualTo(CREATED_AT);

        assertThat(repository.findById(ID))
                .hasValueSatisfying(
                        persisted ->
                                assertThat(persisted).usingRecursiveComparison().isEqualTo(board));
    }

    @Test
    @DisplayName("test 프로파일로 저장하면 id가 이미 존재하는 게시물에서는 행을 추가하지 않고 기존 행을 갱신한다")
    void updatesExistingRowWithoutDuplicateWhenSavingWithExistingId() {
        Board first = Board.of(ID, "게시판", "설명", CREATED_AT);
        savePort.save(first);
        entityManager.flush();

        Board renamed = Board.of(ID, "이름이 바뀐 게시판", "설명", CREATED_AT);
        Board saved = savePort.save(renamed);
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.count()).isEqualTo(1);
        assertThat(repository.findById(ID))
                .hasValueSatisfying(
                        persisted ->
                                assertThat(persisted)
                                        .usingRecursiveComparison()
                                        .isEqualTo(renamed));
        assertThat(saved.getName()).isEqualTo("이름이 바뀐 게시판");
    }

    @Test
    @DisplayName("test 프로파일로 저장할 게시물이 null이면 InvalidDataAccessApiUsageException을 던진다")
    void throwsInvalidDataAccessApiUsageExceptionWhenBoardToSaveIsNull() {
        assertThatThrownBy(() -> savePort.save(null))
                .isInstanceOf(InvalidDataAccessApiUsageException.class);
    }

    @Test
    @DisplayName("저장한 게시물을 findById로 조회하면 같은 값으로 반환한다")
    void findsSavedBoardById() {
        Board board = Board.of(ID, "자유게시판", "자유롭게 이야기를 나눠요", CREATED_AT);
        savePort.save(board);
        entityManager.flush();
        entityManager.clear();

        assertThat(findPort.findById(ID))
                .hasValueSatisfying(
                        found -> assertThat(found).usingRecursiveComparison().isEqualTo(board));
    }

    @Test
    @DisplayName("존재하지 않는 id로 조회하면 Optional.empty를 반환한다")
    void returnsEmptyWhenBoardNotFoundById() {
        assertThat(findPort.findById(UUID.randomUUID())).isEmpty();
    }

    @Test
    @DisplayName("포트로 게시판을 삭제하면 포트 조회에서 제외되지만 행과 삭제 시각은 유지한다")
    void excludesDeletedBoardFromPortFindByIdAndKeepsRow() {
        Board board = Board.of(ID, "자유게시판", "설명", CREATED_AT);
        Board remaining =
                Board.of(
                        UUID.fromString("00000000-0000-0000-0000-000000000002"),
                        "남은 게시판",
                        "설명",
                        CREATED_AT);
        savePort.save(board);
        savePort.save(remaining);
        entityManager.flush();

        board.delete(DELETED_AT);
        savePort.save(board);
        entityManager.flush();
        entityManager.clear();

        assertThat(findPort.findById(ID)).isEmpty();
        assertThat(findPort.findById(remaining.getId()))
                .hasValueSatisfying(
                        persisted ->
                                assertThat(persisted)
                                        .usingRecursiveComparison()
                                        .isEqualTo(remaining));
        assertThat(repository.findById(ID))
                .hasValueSatisfying(
                        persisted -> assertThat(persisted.getDeletedAt()).isEqualTo(DELETED_AT));
    }

    @Test
    @DisplayName("목록 조회는 삭제된 게시판을 제외하고 삭제되지 않은 게시판만 전체 개수에 포함한다")
    void excludesDeletedBoardFromListAndTotalElements() {
        Board deleted = Board.of(ID, "삭제 대상", "설명", CREATED_AT);
        Board remaining =
                Board.of(
                        UUID.fromString("00000000-0000-0000-0000-000000000002"),
                        "남은 게시판",
                        "설명",
                        CREATED_AT.plusSeconds(1));
        savePort.save(deleted);
        savePort.save(remaining);
        entityManager.flush();

        deleted.delete(DELETED_AT);
        savePort.save(deleted);
        entityManager.flush();
        entityManager.clear();

        var page = listPort.findAll(OffsetPageRequest.of(0, 2));

        assertThat(page.items()).extracting(Board::getName).containsExactly("남은 게시판");
        assertThat(page.totalElements()).isEqualTo(1);
    }

    @ParameterizedTest
    @CsvSource({"0,최신,true", "1,동시 생성,true", "2,이전,false"})
    @DisplayName("삭제된 행이 정렬 순서 사이에 있어도 페이지 오프셋과 전체 개수는 남은 게시판만 기준으로 계산한다")
    void paginatesOnlyRemainingBoards(int pageNumber, String expectedName, boolean hasNext) {
        savePageFixtures();
        Board newestDeleted =
                Board.of(
                        UUID.fromString("00000000-0000-0000-0000-000000000004"),
                        "삭제된 최신 게시판",
                        "설명",
                        CREATED_AT.plusSeconds(2));
        Board middleDeleted =
                Board.of(
                        UUID.fromString("00000000-0000-0000-0000-000000000005"),
                        "삭제된 중간 게시판",
                        "설명",
                        CREATED_AT);
        newestDeleted.delete(DELETED_AT);
        middleDeleted.delete(DELETED_AT);
        savePort.save(newestDeleted);
        savePort.save(middleDeleted);
        entityManager.flush();
        entityManager.clear();

        var page = listPort.findAll(OffsetPageRequest.of(pageNumber, 1));

        assertThat(page.items()).extracting(Board::getName).containsExactly(expectedName);
        assertThat(page.page()).isEqualTo(pageNumber);
        assertThat(page.size()).isEqualTo(1);
        assertThat(page.totalElements()).isEqualTo(3);
        assertThat(page.totalPages()).isEqualTo(3);
        assertThat(page.hasNext()).isEqualTo(hasNext);
        assertThat(page.hasPrevious()).isEqualTo(pageNumber > 0);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    @DisplayName("모든 게시판이 삭제되면 첫 페이지와 범위 밖 페이지 모두 빈 목록과 전체 개수 0을 반환한다")
    void returnsEmptyPageWhenAllBoardsAreDeleted(int pageNumber) {
        Board deleted = Board.of(ID, "삭제된 게시판", "설명", CREATED_AT);
        deleted.delete(DELETED_AT);
        savePort.save(deleted);
        entityManager.flush();
        entityManager.clear();

        var page = listPort.findAll(OffsetPageRequest.of(pageNumber, 1));

        assertThat(page.items()).isEmpty();
        assertThat(page.page()).isEqualTo(pageNumber);
        assertThat(page.totalElements()).isZero();
        assertThat(page.totalPages()).isZero();
        assertThat(page.hasNext()).isFalse();
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("목록 첫 페이지는 생성 시각과 ID 내림차순으로 조회하고 전체 개수를 반환한다")
    void findsFirstPageInStableOrder() {
        savePageFixtures();

        var page = listPort.findAll(OffsetPageRequest.of(0, 2));

        assertThat(page.items()).extracting(Board::getName).containsExactly("최신", "동시 생성");
        assertThat(page.page()).isZero();
        assertThat(page.size()).isEqualTo(2);
        assertThat(page.totalElements()).isEqualTo(3);
        assertThat(page.totalPages()).isEqualTo(2);
        assertThat(page.hasNext()).isTrue();
    }

    @Test
    @DisplayName("마지막 페이지는 오프셋 이후 남은 항목과 전체 개수를 반환한다")
    void findsLastPage() {
        savePageFixtures();

        var page = listPort.findAll(OffsetPageRequest.of(1, 2));

        assertThat(page.items()).extracting(Board::getName).containsExactly("이전");
        assertThat(page.page()).isEqualTo(1);
        assertThat(page.size()).isEqualTo(2);
        assertThat(page.totalElements()).isEqualTo(3);
        assertThat(page.hasPrevious()).isTrue();
        assertThat(page.hasNext()).isFalse();
    }

    @Test
    @DisplayName("게시판이 없으면 빈 목록과 전체 개수 0을 반환한다")
    void findsEmptyPage() {
        var page = listPort.findAll(OffsetPageRequest.of(0, 2));

        assertThat(page.items()).isEmpty();
        assertThat(page.totalElements()).isZero();
        assertThat(page.totalPages()).isZero();
        assertThat(page.hasNext()).isFalse();
    }

    @Test
    @DisplayName("마지막 페이지를 벗어나면 빈 목록과 실제 전체 개수를 반환한다")
    void findsPageBeyondLast() {
        savePageFixtures();

        var page = listPort.findAll(OffsetPageRequest.of(2, 2));

        assertThat(page.items()).isEmpty();
        assertThat(page.page()).isEqualTo(2);
        assertThat(page.size()).isEqualTo(2);
        assertThat(page.totalElements()).isEqualTo(3);
        assertThat(page.hasNext()).isFalse();
    }

    private void savePageFixtures() {
        savePort.save(Board.of(ID, "이전", "설명", CREATED_AT));
        savePort.save(
                Board.of(
                        UUID.fromString("00000000-0000-0000-0000-000000000003"),
                        "최신",
                        "설명",
                        CREATED_AT.plusSeconds(1)));
        savePort.save(
                Board.of(
                        UUID.fromString("00000000-0000-0000-0000-000000000002"),
                        "동시 생성",
                        "설명",
                        CREATED_AT));
        entityManager.flush();
        entityManager.clear();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = Board.class)
    @EnableJpaRepositories(basePackageClasses = BoardRepository.class)
    static class TestApplication {}
}
