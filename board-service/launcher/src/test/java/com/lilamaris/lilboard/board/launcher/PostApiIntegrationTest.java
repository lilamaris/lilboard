package com.lilamaris.lilboard.board.launcher;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import com.lilamaris.lilboard.board.domain.Board;
import com.lilamaris.lilboard.board.domain.Post;
import com.lilamaris.lilboard.board.jpa.repository.BoardRepository;
import com.lilamaris.lilboard.board.jpa.repository.PostRepository;
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
class PostApiIntegrationTest {
    private static final UUID BOARD_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID POST_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");

    @LocalServerPort private int port;

    @Autowired private BoardRepository boardRepository;

    @Autowired private PostRepository postRepository;

    @Test
    @DisplayName("삭제되지 않은 게시판의 게시글은 HTTP 조회에서 200과 게시글 정보를 반환한다")
    void findsPostOfRemainingBoardThroughHttp() throws Exception {
        clear();
        boardRepository.saveAndFlush(Board.of(BOARD_ID, "게시판", "설명", CREATED_AT));
        postRepository.saveAndFlush(Post.of(POST_ID, BOARD_ID, "게시글 제목", "본문", CREATED_AT));
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> response =
                    client.send(
                            TestMemberAuthentication.memberRequest(
                                            URI.create(
                                                    "http://localhost:"
                                                            + port
                                                            + "/api/posts/"
                                                            + POST_ID))
                                    .GET()
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());

            assertThat(response.statusCode()).isEqualTo(200);
            var body = JsonPath.parse(response.body());
            assertThat(body.<String>read("$.id")).isEqualTo(POST_ID.toString());
            assertThat(body.<String>read("$.boardId")).isEqualTo(BOARD_ID.toString());
            assertThat(body.<String>read("$.title")).isEqualTo("게시글 제목");
        } finally {
            clear();
        }
    }

    @Test
    @DisplayName("soft delete된 게시판에 HTTP로 게시글을 생성하면 404를 반환하고 게시글 행이 생기지 않는다")
    void rejectsHttpCreateForDeletedBoard() throws Exception {
        clear();
        Board deletedBoard = Board.of(BOARD_ID, "삭제 대상 게시판", "설명", CREATED_AT);
        deletedBoard.delete(CREATED_AT.plusSeconds(10));
        boardRepository.saveAndFlush(deletedBoard);
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> response =
                    client.send(
                            TestMemberAuthentication.memberRequest(
                                            URI.create("http://localhost:" + port + "/api/posts"))
                                    .header("Content-Type", "application/json")
                                    .POST(
                                            HttpRequest.BodyPublishers.ofString(
                                                    "{\"boardId\":\""
                                                            + BOARD_ID
                                                            + "\",\"title\":\"게시글 제목\",\"content\":\"본문\"}"))
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());

            assertThat(response.statusCode()).isEqualTo(404);
            assertThat(postRepository.count()).isZero();
            assertThat(boardRepository.findById(BOARD_ID))
                    .hasValueSatisfying(
                            persisted -> assertThat(persisted.getDeletedAt()).isNotNull());
        } finally {
            clear();
        }
    }

    @Test
    @DisplayName("삭제되지 않은 게시판에 HTTP로 게시글을 생성하면 201과 게시글이 저장된다")
    void createsPostForRemainingBoardThroughHttp() throws Exception {
        clear();
        boardRepository.saveAndFlush(Board.of(BOARD_ID, "남은 게시판", "설명", CREATED_AT));
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> response =
                    client.send(
                            TestMemberAuthentication.memberRequest(
                                            URI.create("http://localhost:" + port + "/api/posts"))
                                    .header("Content-Type", "application/json")
                                    .POST(
                                            HttpRequest.BodyPublishers.ofString(
                                                    "{\"boardId\":\""
                                                            + BOARD_ID
                                                            + "\",\"title\":\"게시글 제목\",\"content\":\"본문\"}"))
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());

            assertThat(response.statusCode()).isEqualTo(201);
            var body = JsonPath.parse(response.body());
            assertThat(body.<String>read("$.boardId")).isEqualTo(BOARD_ID.toString());
            assertThat(body.<String>read("$.title")).isEqualTo("게시글 제목");
            assertThat(postRepository.findAll())
                    .singleElement()
                    .satisfies(persisted -> assertThat(persisted.getTitle()).isEqualTo("게시글 제목"));
        } finally {
            clear();
        }
    }

    @Test
    @DisplayName("soft delete된 게시판의 게시글은 HTTP 조회에서 404를 반환하고 행은 유지한다")
    void returnsNotFoundForPostOfDeletedBoardThroughHttp() throws Exception {
        clear();
        Board board = Board.of(BOARD_ID, "게시판", "설명", CREATED_AT);
        board.delete(CREATED_AT.plusSeconds(10));
        boardRepository.saveAndFlush(board);
        postRepository.saveAndFlush(Post.of(POST_ID, BOARD_ID, "게시글 제목", "본문", CREATED_AT));
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> response =
                    client.send(
                            TestMemberAuthentication.memberRequest(
                                            URI.create(
                                                    "http://localhost:"
                                                            + port
                                                            + "/api/posts/"
                                                            + POST_ID))
                                    .GET()
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());

            assertThat(response.statusCode()).isEqualTo(404);
            assertThat(postRepository.findById(POST_ID)).isPresent();
            assertThat(boardRepository.findById(BOARD_ID))
                    .hasValueSatisfying(
                            persisted -> assertThat(persisted.getDeletedAt()).isNotNull());
        } finally {
            clear();
        }
    }

    @Test
    @DisplayName("soft delete된 게시판의 게시글을 HTTP로 수정하면 404를 반환하고 게시글 행은 원본 그대로 유지한다")
    void rejectsHttpUpdateForPostOfDeletedBoard() throws Exception {
        clear();
        Board deletedBoard = Board.of(BOARD_ID, "삭제 대상 게시판", "설명", CREATED_AT);
        Instant deletedAt = CREATED_AT.plusSeconds(10);
        deletedBoard.delete(deletedAt);
        boardRepository.saveAndFlush(deletedBoard);
        postRepository.saveAndFlush(Post.of(POST_ID, BOARD_ID, "게시글 제목", "본문", CREATED_AT));
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> response =
                    client.send(
                            TestMemberAuthentication.memberRequest(
                                            URI.create(
                                                    "http://localhost:"
                                                            + port
                                                            + "/api/posts/"
                                                            + POST_ID))
                                    .header("Content-Type", "application/json")
                                    .PUT(
                                            HttpRequest.BodyPublishers.ofString(
                                                    "{\"title\":\"새 제목\",\"content\":\"새 본문\"}"))
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());

            assertThat(response.statusCode()).isEqualTo(404);
            assertThat(postRepository.findById(POST_ID))
                    .hasValueSatisfying(
                            persisted -> {
                                assertThat(persisted.getTitle()).isEqualTo("게시글 제목");
                                assertThat(persisted.getContent()).isEqualTo("본문");
                                assertThat(persisted.getUpdatedAt()).isNull();
                            });
            assertThat(boardRepository.findById(BOARD_ID))
                    .hasValueSatisfying(
                            persisted -> assertThat(persisted.getDeletedAt()).isEqualTo(deletedAt));
        } finally {
            clear();
        }
    }

    @Test
    @DisplayName("soft delete된 게시판의 게시글은 목록 API에서 제외되고 전체 개수에 포함되지 않으며 행은 유지한다")
    void excludesPostOfDeletedBoardFromHttpList() throws Exception {
        clear();
        UUID deletedBoardId = UUID.fromString("00000000-0000-0000-0000-000000000003");
        UUID deletedPostId = UUID.fromString("00000000-0000-0000-0000-000000000004");
        Board deletedBoard = Board.of(deletedBoardId, "삭제 대상 게시판", "설명", CREATED_AT);
        deletedBoard.delete(CREATED_AT.plusSeconds(10));
        boardRepository.saveAndFlush(deletedBoard);
        boardRepository.saveAndFlush(Board.of(BOARD_ID, "남은 게시판", "설명", CREATED_AT));
        postRepository.saveAndFlush(
                Post.of(deletedPostId, deletedBoardId, "삭제된 게시글", "본문", CREATED_AT));
        postRepository.saveAndFlush(Post.of(POST_ID, BOARD_ID, "남은 게시글", "본문", CREATED_AT));
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> response =
                    client.send(
                            TestMemberAuthentication.memberRequest(
                                            URI.create(
                                                    "http://localhost:"
                                                            + port
                                                            + "/api/posts?page=0&size=1"))
                                    .GET()
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());

            assertThat(response.statusCode()).isEqualTo(200);
            var body = JsonPath.parse(response.body());
            assertThat(body.<List<String>>read("$.items[*].id"))
                    .containsExactly(POST_ID.toString());
            assertThat(body.<Integer>read("$.totalElements")).isEqualTo(1);
            assertThat(postRepository.findById(deletedPostId)).isPresent();
            assertThat(boardRepository.findById(deletedBoardId))
                    .hasValueSatisfying(
                            persisted -> assertThat(persisted.getDeletedAt()).isNotNull());
        } finally {
            clear();
        }
    }

    private void clear() {
        postRepository.deleteAll();
        boardRepository.deleteAll();
    }
}
