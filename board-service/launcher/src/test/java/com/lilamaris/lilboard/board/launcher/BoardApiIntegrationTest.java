package com.lilamaris.lilboard.board.launcher;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import com.lilamaris.lilboard.board.domain.Board;
import com.lilamaris.lilboard.board.jpa.repository.BoardRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@org.springframework.context.annotation.Import(TestMemberAuthentication.class)
class BoardApiIntegrationTest {
    @LocalServerPort private int port;
    @Autowired private BoardRepository repository;

    @Test
    @DisplayName("launcher의 HTTP 생성 API를 호출하면 유스케이스와 JPA를 거쳐 게시판이 저장된다")
    void createsAndPersistsBoardThroughHttp() throws Exception {
        repository.deleteAll();
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request =
                    TestMemberAuthentication.memberRequest(
                                    URI.create("http://localhost:" + port + "/api/boards"))
                            .header("Content-Type", "application/json")
                            .POST(
                                    HttpRequest.BodyPublishers.ofString(
                                            "{\"name\":\"게시판\",\"description\":\"설명\"}"))
                            .build();
            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(201);
            assertThat(repository.findAll())
                    .singleElement()
                    .satisfies(
                            board -> {
                                assertThat(board.getName()).isEqualTo("게시판");
                                assertThat(board.getDescription()).isEqualTo("설명");
                                assertThat(board.getCreatedAt()).isNotNull();
                                assertThat(board.getId().version()).isEqualTo(7);
                                assertThat(response.body()).contains(board.getId().toString());
                            });
        } finally {
            repository.deleteAll();
        }
    }

    @Test
    @DisplayName("HTTP DELETE API로 게시판을 soft delete하면 삭제 시각이 기록되고 조회 API에서는 제외된다")
    void softDeletesBoardThroughHttpAndExcludesItFromReads() throws Exception {
        repository.deleteAll();
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> created =
                    client.send(
                            TestMemberAuthentication.memberRequest(
                                            URI.create("http://localhost:" + port + "/api/boards"))
                                    .header("Content-Type", "application/json")
                                    .POST(
                                            HttpRequest.BodyPublishers.ofString(
                                                    "{\"name\":\"게시판\",\"description\":\"설명\"}"))
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());
            assertThat(created.statusCode()).isEqualTo(201);
            UUID id = repository.findAll().get(0).getId();

            HttpResponse<String> deleted =
                    client.send(
                            TestMemberAuthentication.memberRequest(
                                            URI.create(
                                                    "http://localhost:"
                                                            + port
                                                            + "/api/boards/"
                                                            + id))
                                    .method("DELETE", HttpRequest.BodyPublishers.noBody())
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());
            assertThat(deleted.statusCode()).isEqualTo(204);
            assertThat(deleted.body()).isEmpty();

            assertThat(repository.findById(id))
                    .hasValueSatisfying(
                            persisted -> assertThat(persisted.getDeletedAt()).isNotNull());

            HttpResponse<String> read =
                    client.send(
                            TestMemberAuthentication.memberRequest(
                                            URI.create(
                                                    "http://localhost:"
                                                            + port
                                                            + "/api/boards/"
                                                            + id))
                                    .GET()
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());
            assertThat(read.statusCode()).isEqualTo(404);
        } finally {
            repository.deleteAll();
        }
    }

    @Test
    @DisplayName("이미 soft delete된 게시판은 DELETE API에서 404를 반환하고 첫 삭제 시각을 유지한다")
    void returnsNotFoundForSecondDeleteOfSoftDeletedBoard() throws Exception {
        repository.deleteAll();
        try (HttpClient client = HttpClient.newHttpClient()) {
            client.send(
                    TestMemberAuthentication.memberRequest(
                                    URI.create("http://localhost:" + port + "/api/boards"))
                            .header("Content-Type", "application/json")
                            .POST(
                                    HttpRequest.BodyPublishers.ofString(
                                            "{\"name\":\"게시판\",\"description\":\"설명\"}"))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            UUID id = repository.findAll().get(0).getId();

            HttpRequest deleteRequest =
                    TestMemberAuthentication.memberRequest(
                                    URI.create("http://localhost:" + port + "/api/boards/" + id))
                            .method("DELETE", HttpRequest.BodyPublishers.noBody())
                            .build();
            HttpResponse<String> first =
                    client.send(deleteRequest, HttpResponse.BodyHandlers.ofString());
            assertThat(first.statusCode()).isEqualTo(204);
            Instant firstDeletedAt = repository.findById(id).orElseThrow().getDeletedAt();

            HttpResponse<String> second =
                    client.send(deleteRequest, HttpResponse.BodyHandlers.ofString());
            assertThat(second.statusCode()).isEqualTo(404);

            assertThat(repository.findById(id))
                    .hasValueSatisfying(
                            persisted ->
                                    assertThat(persisted.getDeletedAt()).isEqualTo(firstDeletedAt));
        } finally {
            repository.deleteAll();
        }
    }

    @Test
    @DisplayName("삭제 후 목록 API는 삭제된 게시판을 제외하고 남은 게시판과 전체 개수를 반환한다")
    void excludesDeletedBoardFromHttpList() throws Exception {
        repository.deleteAll();
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        UUID deletedId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID remainingId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        repository.saveAndFlush(Board.of(deletedId, "삭제 대상", "설명", createdAt.plusSeconds(1)));
        repository.saveAndFlush(Board.of(remainingId, "남은 게시판", "설명", createdAt));
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> deleted =
                    client.send(
                            TestMemberAuthentication.memberRequest(
                                            URI.create(
                                                    "http://localhost:"
                                                            + port
                                                            + "/api/boards/"
                                                            + deletedId))
                                    .DELETE()
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());
            assertThat(deleted.statusCode()).isEqualTo(204);

            HttpResponse<String> listed =
                    client.send(
                            TestMemberAuthentication.memberRequest(
                                            URI.create(
                                                    "http://localhost:"
                                                            + port
                                                            + "/api/boards?page=0&size=1"))
                                    .GET()
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());

            assertThat(listed.statusCode()).isEqualTo(200);
            var body = JsonPath.parse(listed.body());
            assertThat(body.<List<String>>read("$.items[*].id"))
                    .containsExactly(remainingId.toString());
            assertThat(body.<Integer>read("$.totalElements")).isEqualTo(1);
            assertThat(repository.count()).isEqualTo(2);
        } finally {
            repository.deleteAll();
        }
    }

    @Test
    @DisplayName("삭제된 게시판을 HTTP로 수정하면 404를 반환하고 삭제 시각과 기존 데이터를 유지한다")
    void rejectsHttpUpdateOfDeletedBoardWithoutChangingData() throws Exception {
        repository.deleteAll();
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        Board board = Board.of(id, "원래 게시판", "원래 설명", createdAt);
        board.updateName("수정된 게시판", createdAt.plusSeconds(1));
        board.delete(createdAt.plusSeconds(2));
        repository.saveAndFlush(board);
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> updated =
                    client.send(
                            TestMemberAuthentication.memberRequest(
                                            URI.create(
                                                    "http://localhost:"
                                                            + port
                                                            + "/api/boards/"
                                                            + id))
                                    .header("Content-Type", "application/json")
                                    .PUT(
                                            HttpRequest.BodyPublishers.ofString(
                                                    "{\"name\":\"새 이름\",\"description\":\"새 설명\"}"))
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());

            assertThat(updated.statusCode()).isEqualTo(404);
            assertThat(repository.findById(id))
                    .hasValueSatisfying(
                            persisted ->
                                    assertThat(persisted)
                                            .usingRecursiveComparison()
                                            .isEqualTo(board));
        } finally {
            repository.deleteAll();
        }
    }
}
