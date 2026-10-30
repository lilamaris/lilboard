package com.lilamaris.lilboard.board.launcher;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.lilamaris.lilboard.board.domain.Board;
import com.lilamaris.lilboard.board.domain.Post;
import com.lilamaris.lilboard.board.jpa.repository.BoardRepository;
import com.lilamaris.lilboard.board.jpa.repository.PostRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test", "loadtest"})
@DisplayName("loadtest 프로파일의 시딩 API 통합 테스트")
class SeedApiIntegrationTest {
    @LocalServerPort private int port;

    @Autowired private BoardRepository boardRepository;

    @Autowired private PostRepository postRepository;

    @BeforeEach
    @AfterEach
    void clear() {
        postRepository.deleteAll();
        boardRepository.deleteAll();
    }

    @Test
    @DisplayName("시딩 API를 호출하면 요청한 개수만큼 게시판과 게시글이 저장되고 결과가 반환된다")
    void seedsRequestedCountsThroughHttp() throws Exception {
        HttpResponse<String> response = post("/api/seed?boards=3&postsPerBoard=2");

        assertThat(response.statusCode()).isEqualTo(200);
        DocumentContext body = JsonPath.parse(response.body());
        assertThat(body.read("$.boards", Integer.class)).isEqualTo(3);
        assertThat(body.read("$.posts", Integer.class)).isEqualTo(6);
        assertThat(body.read("$.totalRows", Integer.class)).isEqualTo(9);

        List<UUID> boardIds = boardRepository.findAll().stream().map(Board::getId).toList();
        assertThat(boardRepository.count()).isEqualTo(3);
        assertThat(postRepository.count()).isEqualTo(6);
        assertThat(postRepository.findAll())
                .allSatisfy(post -> assertThat(boardIds).contains(post.getBoardId()));
    }

    @Test
    @DisplayName("파라미터 없이 시딩 API를 호출하면 기본값 100개 게시판과 10개 게시글이 저장된다")
    void seedsDefaultCountsWhenParametersAreOmitted() throws Exception {
        HttpResponse<String> response = post("/api/seed");

        assertThat(response.statusCode()).isEqualTo(200);
        DocumentContext body = JsonPath.parse(response.body());
        assertThat(body.read("$.boards", Integer.class)).isEqualTo(100);
        assertThat(body.read("$.posts", Integer.class)).isEqualTo(1000);
        assertThat(boardRepository.count()).isEqualTo(100);
        assertThat(postRepository.count()).isEqualTo(1000);
    }

    @Test
    @DisplayName("총 행이 상한을 넘는 시딩 요청은 400 ProblemDetail을 반환하고 데이터가 남지 않는다")
    void returnsProblemDetailAndKeepsDatabaseEmptyForRequestsOverUpperBound() throws Exception {
        HttpResponse<String> response = post("/api/seed?boards=100001&postsPerBoard=0");

        assertThat(response.statusCode()).isEqualTo(400);
        DocumentContext body = JsonPath.parse(response.body());
        assertThat(body.read("$.title", String.class)).isEqualTo("잘못된 요청");
        assertThat(body.read("$.detail", String.class)).isEqualTo("must be at most 100000.");
        assertThat(boardRepository.count()).isZero();
        assertThat(postRepository.count()).isZero();
    }

    @Test
    @DisplayName("boards가 0인 시딩 요청은 400 ProblemDetail을 반환하고 데이터가 남지 않는다")
    void returnsProblemDetailForZeroBoardCount() throws Exception {
        HttpResponse<String> response = post("/api/seed?boards=0&postsPerBoard=10");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(JsonPath.parse(response.body()).read("$.detail", String.class))
                .isEqualTo("must be positive.");
        assertThat(boardRepository.count()).isZero();
    }

    @Test
    @DisplayName("시딩 API를 두 번 호출하면 요청한 개수가 누적되어 저장된다")
    void accumulatesRowsAcrossRequests() throws Exception {
        post("/api/seed?boards=2&postsPerBoard=2");
        post("/api/seed?boards=3&postsPerBoard=1");

        assertThat(boardRepository.count()).isEqualTo(5);
        assertThat(postRepository.count()).isEqualTo(7);
    }

    @Test
    @DisplayName("시딩된 게시판은 게시판 목록 API가 생성 시각 내림차순으로 반환한다")
    void listsSeededBoardsInCreatedAtDescendingOrder() throws Exception {
        post("/api/seed?boards=3&postsPerBoard=0");

        HttpResponse<String> response = get("/api/boards?page=0&size=20");
        DocumentContext body = JsonPath.parse(response.body());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(body.read("$.totalElements", Integer.class)).isEqualTo(3);
        assertThat(body.<List<String>>read("$.items[*].name"))
                .containsExactly("board-000002", "board-000001", "board-000000");
    }

    @Test
    @DisplayName("시딩된 게시글은 게시글 목록 API에서 페이지로 반환된다")
    void listsSeededPostsWithPagingApi() throws Exception {
        post("/api/seed?boards=2&postsPerBoard=2");

        HttpResponse<String> response = get("/api/posts?page=0&size=10");
        DocumentContext body = JsonPath.parse(response.body());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(body.read("$.totalElements", Integer.class)).isEqualTo(4);
        assertThat(body.<List<String>>read("$.items[*].title"))
                .containsExactly(
                        "post-000001-000001",
                        "post-000001-000000",
                        "post-000000-000001",
                        "post-000000-000000");
        assertThat(postRepository.findAll()).extracting(Post::getTitle).hasSize(4);
    }

    private HttpResponse<String> post(String path) throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            return client.send(
                    HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.noBody())
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
        }
    }

    private HttpResponse<String> get(String path) throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            return client.send(
                    HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
        }
    }
}
