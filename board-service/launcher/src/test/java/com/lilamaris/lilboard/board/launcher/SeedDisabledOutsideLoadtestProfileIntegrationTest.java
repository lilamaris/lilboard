package com.lilamaris.lilboard.board.launcher;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import com.lilamaris.lilboard.board.jpa.repository.BoardRepository;
import com.lilamaris.lilboard.board.seed.SeedController;
import com.lilamaris.lilboard.board.seed.SeedService;
import com.lilamaris.lilboard.board.seed.SeedStore;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("loadtest 밖의 프로파일에서 시딩이 비활성인 통합 테스트")
class SeedDisabledOutsideLoadtestProfileIntegrationTest {
    @LocalServerPort private int port;

    @Autowired private ApplicationContext context;

    @Autowired private BoardRepository boardRepository;

    @Test
    @DisplayName("loadtest가 없는 test 프로파일에서는 시딩 빈이 등록되지 않는다")
    void hasNoSeedBeansWithoutLoadtestProfile() {
        assertThat(context.getBeansOfType(SeedController.class)).isEmpty();
        assertThat(context.getBeansOfType(SeedService.class)).isEmpty();
        assertThat(context.getBeansOfType(SeedStore.class)).isEmpty();
    }

    @Test
    @DisplayName("loadtest가 없는 프로파일의 시딩 API는 404 ProblemDetail을 반환하고 데이터가 남지 않는다")
    void returnsNotFoundAndKeepsDatabaseEmptyForSeedEndpoint() throws Exception {
        HttpResponse<String> response = post("/api/seed?boards=10&postsPerBoard=10");

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(JsonPath.parse(response.body()).read("$.title", String.class))
                .isEqualTo("요청한 대상을 찾을 수 없습니다");
        assertThat(boardRepository.count()).isZero();
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
}
