package com.lilamaris.lilboard.board.launcher;

import static org.assertj.core.api.Assertions.assertThat;

import com.lilamaris.lilboard.board.jpa.repository.BoardRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class BoardApiIntegrationTest {
    @LocalServerPort private int port;
    @Autowired private BoardRepository repository;

    @Test
    @DisplayName("launcher의 HTTP 생성 API를 호출하면 유스케이스와 JPA를 거쳐 게시판이 저장된다")
    void createsAndPersistsBoardThroughHttp() throws Exception {
        repository.deleteAll();
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request =
                    HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/boards"))
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
}
