package com.lilamaris.lilboard.board.launcher;

import static org.assertj.core.api.Assertions.assertThat;

import com.lilamaris.lilboard.board.jpa.repository.BoardRepository;
import com.lilamaris.lilboard.board.seed.SeedController;
import com.lilamaris.lilboard.board.seed.SeedService;
import com.lilamaris.lilboard.board.seed.SeedStore;
import com.lilamaris.lilboard.board.web.security.HttpAuthorizationCustomizer;
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
@org.springframework.context.annotation.Import(TestMemberAuthentication.class)
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
        assertThat(context.getBeansOfType(HttpAuthorizationCustomizer.class)).isEmpty();
    }

    @Test
    @DisplayName("loadtest가 없는 프로파일의 비로그인 시딩 요청은 401로 차단되고 데이터가 남지 않는다")
    void rejectsAnonymousSeedRequestAndKeepsDatabaseEmpty() throws Exception {
        HttpResponse<String> response = post("/api/seed?boards=10&postsPerBoard=10");

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(boardRepository.count()).isZero();
    }

    @Test
    @DisplayName("loadtest가 없으면 회원 JWT를 사용해도 시딩 경로는 403으로 차단된다")
    void rejectsMemberSeedRequestWithoutLoadtestProfile() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            var request =
                    TestMemberAuthentication.memberRequest(
                                    URI.create(
                                            "http://localhost:"
                                                    + port
                                                    + "/api/seed?boards=1&postsPerBoard=1"))
                            .POST(HttpRequest.BodyPublishers.noBody())
                            .build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(403);
            assertThat(boardRepository.count()).isZero();
        }
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
