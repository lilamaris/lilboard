package com.lilamaris.lilboard.board.jpa.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.lilamaris.lilboard.board.domain.Board;
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
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = BoardRepositoryTest.TestApplication.class)
class BoardRepositoryTest {

    @Autowired private BoardRepository repository;

    @Autowired private EntityManager entityManager;

    @Test
    @DisplayName("test 프로파일에서 게시판을 저장하면 H2 스키마에서 동일한 값으로 조회한다")
    void savesAndLoadsBoardWithTestProfile() {
        Board board =
                Board.of(
                        UUID.fromString("00000000-0000-0000-0000-000000000001"),
                        "게시판",
                        "설명",
                        Instant.parse("2026-01-01T00:00:00Z"));

        repository.saveAndFlush(board);
        entityManager.clear();

        assertThat(repository.findById(board.getId()))
                .hasValueSatisfying(
                        actual -> assertThat(actual).usingRecursiveComparison().isEqualTo(board));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = Board.class)
    @EnableJpaRepositories(basePackageClasses = BoardRepository.class)
    static class TestApplication {}
}
