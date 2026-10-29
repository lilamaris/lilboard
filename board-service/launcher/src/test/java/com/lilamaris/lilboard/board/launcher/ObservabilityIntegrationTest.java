package com.lilamaris.lilboard.board.launcher;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.tracing.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
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
class ObservabilityIntegrationTest {
    @LocalServerPort private int port;

    @Autowired private ApplicationContext applicationContext;

    @Test
    @DisplayName("health 엔드포인트가 노출되고 상태 UP을 반환한다")
    void exposesHealthEndpoint() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> response =
                    client.send(
                            HttpRequest.newBuilder(
                                            URI.create(
                                                    "http://localhost:"
                                                            + port
                                                            + "/actuator/health"))
                                    .GET()
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).contains("\"status\":\"UP\"");
        }
    }

    @Test
    @DisplayName("prometheus 엔드포인트는 HTTP 요청 메트릭과 게시판 SELECT의 DB 메트릭을 노출한다")
    void exposesHttpAndDbMetricsThroughPrometheus() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> api =
                    client.send(
                            HttpRequest.newBuilder(
                                            URI.create(
                                                    "http://localhost:"
                                                            + port
                                                            + "/api/boards?page=0&size=20"))
                                    .GET()
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());
            assertThat(api.statusCode()).isEqualTo(200);

            HttpResponse<String> scrape =
                    client.send(
                            HttpRequest.newBuilder(
                                            URI.create(
                                                    "http://localhost:"
                                                            + port
                                                            + "/actuator/prometheus"))
                                    .GET()
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());

            assertThat(scrape.statusCode()).isEqualTo(200);
            assertThat(scrape.body())
                    .contains("http_server_requests_seconds_count")
                    .contains(
                            "db_client_operation_duration_seconds_count"
                                    + "{db_collection_name=\"board\",db_operation_name=\"SELECT\"");
        }
    }

    @Test
    @DisplayName("tracing과 OpenTelemetry SDK가 기동 시 등록된다")
    void registersTracerAndOpenTelemetrySdk() {
        assertThat(applicationContext.getBeansOfType(Tracer.class)).isNotEmpty();
        assertThat(applicationContext.getBeansOfType(OpenTelemetrySdk.class)).isNotEmpty();
    }
}
