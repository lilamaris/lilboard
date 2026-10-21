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

    @Test
    @DisplayName("H2의 동일 테이블에서 UUIDv4와 UUIDv7 게시판을 각각 저장하고 조회한다")
    void savesAndLoadsVersionFourAndSevenUuidsTogether() {
        UUID legacyId = UUID.fromString("c9bf9a5c-7a33-4b0e-9c8a-1f2e3d4c5b6a");
        UUID newId = UUID.fromString("019a0c00-0000-7000-8000-000000000001");
        Board legacy = Board.of(legacyId, "기존 게시판", "설명", Instant.parse("2026-01-01T00:00:00Z"));
        Board current = Board.of(newId, "신규 게시판", "설명", Instant.parse("2026-01-02T00:00:00Z"));
        repository.saveAndFlush(legacy);
        repository.saveAndFlush(current);
        entityManager.clear();

        assertThat(repository.findById(legacyId))
                .hasValueSatisfying(
                        actual -> assertThat(actual).usingRecursiveComparison().isEqualTo(legacy));
        assertThat(repository.findById(newId))
                .hasValueSatisfying(
                        actual -> assertThat(actual).usingRecursiveComparison().isEqualTo(current));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = Board.class)
    @EnableJpaRepositories(basePackageClasses = BoardRepository.class)
    static class TestApplication {}
}
