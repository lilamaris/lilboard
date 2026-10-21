package com.lilamaris.lilboard.board.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lilamaris.lilboard.board.application.port.out.BoardSavePort;
import com.lilamaris.lilboard.board.domain.Board;
import com.lilamaris.lilboard.board.jpa.repository.BoardRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
@ContextConfiguration(classes = BoardSavePortJpaAdapterTest.TestApplication.class)
@Import(BoardSavePortJpaAdapter.class)
@DisplayName("BoardSavePortJpaAdapter 테스트")
class BoardSavePortJpaAdapterTest {
    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired private BoardSavePort savePort;

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

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = Board.class)
    @EnableJpaRepositories(basePackageClasses = BoardRepository.class)
    static class TestApplication {}
}
