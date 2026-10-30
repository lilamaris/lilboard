# 부하 테스트 시딩

[DevOps 체크리스트](../checklist.md)의 상세 설정과 검증 근거를 기록한다. 설정 값·확인 일자·실행 결과와 미확인 범위를 남긴다.

## 구성

대상: `loadtest` 프로파일과 시딩 API (`Board`·`Post`)

`board-service:seed` 모듈과 `loadtest` 프로파일로 부하 테스트 데이터를 생성한다. 시딩은 `POST /api/seed` 요청으로 수행하고, 개수는 요청 파라미터로 유연하게 지정한다.

| 항목 | 값 |
|---|---|
| 엔드포인트 | `POST /api/seed` |
| 파라미터 | `boards` (기본 100), `postsPerBoard` (기본 10) |
| 총 행 상한 | `board.seed.max-total-rows` (기본 100000) = `boards × (1 + postsPerBoard)` |
| JDBC 배치 크기 | `board.seed.batch-size` (기본 500) |
| 활성화 프로파일 | `loadtest` |

- `loadtest`가 없으면 `SeedController`·`SeedService`·`SeedStore`·`SeedLimits` 빈이 등록되지 않는다. `prod`와 `test`에서는 시딩 엔드포인트가 존재하지 않고 요청은 404 ProblemDetail로 처리된다.
- 총 행 상한을 넘는 요청과 `boards ≤ 0`, `postsPerBoard < 0`은 저장 전에 `IllegalComparisonConditionException`·`IllegalValueConditionException`으로 실패하고 400 ProblemDetail을 반환한다. 실패 시 데이터가 남지 않는다.
- 게시판·게시글은 JDBC 배치로 기록한다. 게시판 행을 먼저 기록하고 게시글을 기록하므로 `post.board_id` 외래 키가 유지된다.
- 생성 시각는 시딩 시각을 가장 최근 행으로 두고 행마다 1초씩 앞선다. 게시글은 자기 게시판보다 최근 시각을 가진다.
- Comment는 `main`에 병합되지 않아 시딩 대상에 포함하지 않는다. Comment 병합 후 확장한다.

## 검증 기록

| 구분 | 내용 |
|---|---|
| 검증 일자 | 2026-10-30 (Asia/Seoul), `feat/load-test-seed` 작업 트리에서 검증 |
| 단위 테스트 | `SeedPlanTest`, `SeedLimitsTest`, `SeedDataGeneratorTest`, `SeedServiceTest`, `SeedControllerTest` — 상한·배치 검증, 개수·이름·생성 시각 규칙, Clock 제어, 파라미터 기본값과 검증 위임 |
| H2 통합 테스트 | `JdbcSeedStoreIntegrationTest`, `SeedConfigurationIntegrationTest`, `SeedConfigurationWithoutLoadtestIntegrationTest`, `SeedLimitsPropertyIntegrationTest` — 배치 저장과 컬럼 값, `loadtest` 빈 등록과 기본 상한, `test` 단독 프로파일에서 빈 없음, 상한·배치 설정 overriding |
| API 통합 테스트 | `SeedApiIntegrationTest` — HTTP 호출로 요청 개수 저장·반환, 기본값 100/10, 상한 초과와 `boards=0`의 400 ProblemDetail과 데이터 없음, 누적 저장, 게시판·게시글 목록 API 노출 |
| 프로파일 게이트 | `SeedDisabledOutsideLoadtestProfileIntegrationTest` — `test` 단독에서 시딩 빈 없음과 시딩 요청 404, 데이터 없음 |
| PostgreSQL 통합 테스트 | `SeedPostgresProfileIntegrationTest` — `prod` 단독에서 시딩 빈 없음·Flyway pending 없음, `prod + loadtest`에서 Flyway 스키마에 행 저장과 외래 키 무결성, `created_at`가 `timestamp with time zone`에 초 간격으로 저장, `deleted_at` 없는 게시판 행과 생성 순서 |
| 명령 | `./gradlew :board-service:seed:test :board-service:launcher:test :board-service:web:test :board-service:jpa:test :board-service:application:test :kernel:test spotlessCheck --console=plain` |
| 결과 | seed 43개, launcher 37개 성공(실패·오류·skip 0). 함께 실행한 kernel 221개, application 77개, domain 74개, web 93개, jpa 33개도 성공하여 전체 593개 테스트가 통과했다. `spotlessCheck`와 모듈별 커버리지 게이트(신규 `seed` 포함) 통과. |
| 아키텍처 | `ArchitectureTest` 4개 성공 — 새 `board.seed` 패키지가 계층 경계 규칙과 launcher 역할 제한 규칙을 위반하지 않음을 확인 |
| 한계 | 실제 부하 테스트 환경(PostgreSQL과 loadtest 기동)에서 시딩을 실행하고 부하 테스트 도구로 데이터를 사용한 검증은 하지 않았다. 원격 CI 결과 확인은 main 병합 후 수행한다. 총 행 상한 100000은 초기값이며 부하 테스트 환경의 용량·실행 시간에 맞춰 조정한다. |

## 사용 방법

1. `loadtest` 프로파일을 PostgreSQL 환경에 추가해 기동한다. 예: `SPRING_PROFILES_ACTIVE=prod,loadtest`.
2. 시딩 실행: `curl -X POST 'http://localhost:8080/api/seed?boards=1000&postsPerBoard=100'`.
3. 응답 예: `{"boards":1000,"posts":100000,"totalRows":101000}`. 상한 초과 응답은 400 ProblemDetail이고 저장되지 않는다.
4. 시딩 데이터는 삭제하지 않으므로 부하 테스트 전용 DB·스키마에서 실행한다.
