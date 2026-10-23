# PostgreSQL과 Flyway 통합 테스트

[DevOps 체크리스트](../checklist.md)의 상세 설정과 검증 근거를 기록한다. 날짜·커밋·실행 결과는 기존 확인 시점의 기록이며, 문서 분리 과정에서 원격 설정이나 런타임을 재검증하지 않았다.

## 실행 프로파일

대상: 실행 프로파일 분리(`prod` PostgreSQL + Flyway / `test` H2)

`e5f4d9b` (실제 PostgreSQL 마이그레이션 검증은 P0에 별도 기록)

## 통합 테스트 구성

대상: Testcontainers PostgreSQL 기반 Flyway 통합 테스트

prod 설정으로 빈 DB 마이그레이션·JPA `validate`, 저장·조회, 같은 DB로 앱 재기동 후 마이그레이션·데이터 유지 검증. [통합 테스트 실행 기록](#통합-테스트-실행-기록) 참조 — `f9871f1`

## 통합 테스트 CI 경로

대상: 통합 테스트 CI 실행 경로 구성

기본 `test`에 포함되어 기존 필수 `Build and test`의 `./gradlew build`에서 실행. Docker 사전 확인 추가, Docker 미사용 시 skip 없이 실패

## 원격 CI 결과

대상: PostgreSQL 통합 테스트 원격 CI 결과 확인

run `39493409122` artifact `test-reports`: `PostgresFlywayIntegrationTest` 포함, testsuite 12개·tests 200 성공. Docker 사전 확인 step 실행 확인(2026-10-21)

## 통합 테스트 실행 기록

| 구분 | 내용 |
|---|---|
| 검증 일자 | 2026-10-21 (Asia/Seoul), `f9871f1`에 반영된 테스트를 커밋 전 작업 트리에서 검증 |
| 테스트 | `board-service/launcher/src/test/java/com/lilamaris/lilboard/board/launcher/PostgresFlywayIntegrationTest.java` |
| DB | Testcontainers `postgres:17.6-alpine`. 현재 Compose도 동일 버전이며, 향후 운영 DB 버전 결정 시 함께 정렬한다. |
| 설정 | 기존 `prod` 프로파일(Flyway 활성화, JPA `validate`)을 사용하며, 컨테이너의 임시 접속 정보를 명령행 인자로 전달한다. H2 `test` 프로파일은 사용하지 않는다. |
| 시나리오 | 빈 DB 최초 기동·V1 적용, 게시판 저장·조회, 같은 DB 컨테이너를 유지하고 앱 컨텍스트 종료·재생성 후 V1 중복 적용 없이 데이터 유지 |
| 명령 | `./gradlew :board-service:launcher:test --tests '*PostgresFlywayIntegrationTest' --console=plain` |
| 결과 | 테스트 3개 성공, 실패·오류·skip 0. 후속 `./gradlew build --console=plain`도 성공하여 기존 테스트·포맷·커버리지 게이트 통과 확인. 보고서: `board-service/launcher/build/reports/tests/test/index.html` |
| CI | 별도 태그·job으로 분리하지 않고 기본 테스트에 포함한다. 컨테이너는 테스트마다 새로 생성·종료하고, 재기동 시나리오에서만 두 앱 컨텍스트가 같은 DB를 사용한다. |
| 보충 | 원격 CI 실행 결과 확인 완료(2026-10-21). run `39493409122` artifact `test-reports`에서 `PostgresFlywayIntegrationTest` 포함, testsuite 12개·tests 200 성공. Docker 사전 확인 step 실행 확인. |
