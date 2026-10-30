package com.lilamaris.lilboard.board.launcher;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@org.springframework.context.annotation.Import(TestMemberAuthentication.class)
class ApiExceptionHandlerIntegrationTest {
    @LocalServerPort private int port;

    @Test
    @DisplayName("전역 예외 처리가 적용된 앱에서 존재하지 않는 게시판 조회는 404 ProblemDetail을 반환한다")
    void returnsProblemDetailForMissingBoard() throws Exception {
        HttpResponse<String> response = get("/api/boards/00000000-0000-0000-0000-000000000099");

        assertThat(response.statusCode()).isEqualTo(404);
        DocumentContext body = JsonPath.parse(response.body());
        assertThat(body.read("$.status", Integer.class)).isEqualTo(404);
        assertThat(body.read("$.title", String.class)).isEqualTo("요청한 대상을 찾을 수 없습니다");
        assertThat(body.read("$.detail", String.class)).isEqualTo("Board를 찾을 수 없습니다.");
        assertThat(body.read("$.instance", String.class))
                .isEqualTo("/api/boards/00000000-0000-0000-0000-000000000099");
    }

    @Test
    @DisplayName("kernel 조건 위반은 앱 계층을 넘어 HTTP 400 ProblemDetail이 된다")
    void returnsProblemDetailForConditionViolation() throws Exception {
        HttpResponse<String> response = get("/api/boards?page=-1&size=20");

        assertThat(response.statusCode()).isEqualTo(400);
        DocumentContext body = JsonPath.parse(response.body());
        assertThat(body.read("$.title", String.class)).isEqualTo("잘못된 요청");
        assertThat(body.read("$.detail", String.class)).isEqualTo("must be non-negative.");
    }

    @Test
    @DisplayName("jakarta 검증 위반은 HTTP 400과 errors에 위반 필드를 담는다")
    void returnsProblemDetailWithFieldErrors() throws Exception {
        HttpResponse<String> response =
                post("/api/boards", "{\"name\":\" \",\"description\":\"설명\"}");

        assertThat(response.statusCode()).isEqualTo(400);
        DocumentContext body = JsonPath.parse(response.body());
        assertThat(body.read("$.detail", String.class)).isEqualTo("요청값이 유효하지 않습니다.");
        assertThat(body.<List<Map<String, String>>>read("$.errors"))
                .anySatisfy(error -> assertThat(error).containsEntry("field", "name"));
    }

    @Test
    @DisplayName("잘못된 경로 변수 형식은 HTTP 400 ProblemDetail이 된다")
    void returnsProblemDetailForMalformedPathVariable() throws Exception {
        HttpResponse<String> response = get("/api/boards/invalid-uuid");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(JsonPath.parse(response.body()).read("$.title", String.class))
                .isEqualTo("잘못된 요청");
    }

    private HttpResponse<String> get(String path) throws Exception {
        return send(TestMemberAuthentication.memberRequest(uri(path)).GET().build());
    }

    private HttpResponse<String> post(String path, String body) throws Exception {
        return send(
                TestMemberAuthentication.memberRequest(uri(path))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build());
    }

    private HttpResponse<String> send(HttpRequest request) throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        }
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }
}
