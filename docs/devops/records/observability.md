# 관찰성 연결

[DevOps 체크리스트](../checklist.md)의 관찰성 항목에 대해 앱 측 구성과 검증 근거를 기록한다. 구성 완료와 실제 수집·시각화 연결을 구분한다.

## 앱 측 구성

대상: `launcher`에 actuator·OTel·Prometheus registry를 컨벤션 플러그인으로 연결

`feat/launcher-observability` 브랜치에서 `build-logic/convention/src/main/kotlin/spring.observability.gradle.kts`를 추가하고 `board-service/launcher/build.gradle.kts`에 적용했다. 카탈로그에 선언만 있던 의존성이 실제 런타임 클래스path에 들어갔다.

- `spring-boot-starter-actuator`, `spring-boot-starter-opentelemetry`, `micrometer-registry-prometheus`
- `datasource-micrometer-spring-boot`, `datasource-micrometer-opentelemetry` (JDBC 메트릭·span)

`application.yml` 설정:

- `management.endpoints.web.exposure.include: health,metrics,prometheus` — Boot 4의 web exposure 기본값은 `health`뿐이다.
- `management.tracing.sampling.probability: ${TRACING_SAMPLING_PROBABILITY:1.0}` — 낮은 트래픽에서 head sampling 1.0.
- `management.otlp.metrics.export.enabled: false` — 메트릭은 Prometheus scrape로 수집하고 OTLP push를 꺼서 중복 전송을 피한다.

Prometheus는 관측성 호스트에 있고 앱 호스트에는 `postgres_exporter`만 둘 계획이다. 앱은 scrape 대상 엔드포인트만 노출한다.

## 엔드포인트를 환경 변수로 바꿀 수 있는 경로

Boot 4.1.2는 표준 OTel 환경 변수를 Boot 속성에 매핑한다. `OTEL_EXPORTER_OTLP_ENDPOINT`는 `management.opentelemetry.tracing.export.otlp.endpoint`(base + `/v1/traces`)와 `management.opentelemetry.logging.export.otlp.endpoint`(base + `/v1/logs`)로 해석된다. 신호별 `OTEL_EXPORTER_OTLP_TRACES_ENDPOINT`, `OTEL_EXPORTER_OTLP_LOGS_ENDPOINT`도 지원한다.

`compose.yaml`은 `OTEL_EXPORTER_OTLP_ENDPOINT`를 그대로 전달하고 `extra_hosts`로 `host.docker.internal`을 추가한다. Boot는 빈 값의 환경 변수를 건너뛰므로 미설정 시 exporter 기본값을 사용한다. 원격 관측성 호스트 주소로 설정하는 것이 권장 경로다.

`OTEL_EXPORTER_OTLP_ENDPOINT`는 `.env.example`에 예시 값으로 기록했다. 실제 `.env`에는 원격 관측성 호스트 주소와, 필요하면 `TRACING_SAMPLING_PROBABILITY`를 넣는다.

## 검증 (2026-10-29)

로컬 검증만 수행했다. 원격 관측성 호스트와의 연결은 확인하지 못했다.

`./gradlew build` 통과. `launcher` 28개 테스트 0 실패. 추가한 `ObservabilityIntegrationTest` 3개:

- `health` 엔드포인트가 `UP`을 반환한다.
- `prometheus` 엔드포인트가 `http_server_requests_seconds_count`와 게시판 SELECT의 `db_client_operation_duration_seconds_count{db_collection_name="board",db_operation_name="SELECT"}`를 노출한다.
- 기동 시 `Tracer`와 `OpenTelemetrySdk`가 등록된다.

Compose(prod + PostgreSQL)로 앱 컨테이너를 띄우고 확인했다. `BOARD_PORT=8081`을 사용했다.

- `/actuator/health`는 `{"groups":["liveness","readiness"],"status":"UP"}`를 반환했다.
- `/actuator/prometheus`는 `http_server_requests_seconds_count{method="POST",status="201",uri="/api/boards"}`, `hikaricp_connections*`, `db_client_operation_duration_seconds*`, `jdbc_connection_*`를 노출했다.
- 로컬 OTLP 리스너(`host.docker.internal:4319`)는 `/v1/traces` 배치를 받았다. payload에 `http post /api/boards`, `http get /api/boards`, `SELECT board`, `INSERT board`와 실제 SQL 텍스트, `service.name=lilboard-launcher`, `jdbc.datasource.name`, `jdbc.row-count`가 포함됐다.

## 확인하지 못한 항목

- **앱 로그는 OTLP로 전송되지 않는다.** Boot 4.1.2는 `SdkLoggerProvider`와 OTLP log exporter를 구성하지만 Logback 1.5.38에 OTLP appender가 없고, Boot 4.1.2 jar에도 appender가 없다. 리스너는 `/v1/logs`를 받지 못했다. Loki 수집에는 stdout를 꼬리는 collector(Alloy·promtail) 또는 OTel Java agent가 필요하다. JSON 구조화 로그 항목과 함께 결정한다.
- Prometheus scrape와 Grafana 시각화가 실제 관측성 호스트에서 작동하는지 확인하지 못했다. 리스너는 로컬에서 트래픽 수신만 확인한 도구다.
- 관리 엔드포인트 접근 제한을 확인하지 못했다. Compose 기본 바인딩은 `127.0.0.1`이므로 다른 호스트의 Prometheus가 `/actuator/prometheus`를 scrape하려면 두 호스트 사이의 네트워크로 앱 포트 접근을 허용하는 결정이 필요하다.
- `postgres_exporter` 구성과 DB 메트릭 수집은 앱 호스트 측 작업으로 아직 구성하지 않았다.
