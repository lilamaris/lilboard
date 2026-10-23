# JVM에서 직접 실행

Java 21과 Gradle wrapper로 board-service를 실행한다. 모든 명령은 저장소 루트 기준이며, 접속 가능한 PostgreSQL이 필요하다.

## 프로파일

- 공통 설정: `board-service/launcher/src/main/resources/application.yml`
- `prod`: PostgreSQL + Flyway로 스키마를 관리하며, JPA는 스키마를 검증합니다.
- `test`: H2 + JPA `create-drop`을 사용하며 Flyway는 비활성화합니다.
  각 모듈의 `src/test/resources/application-test.yml`에 두고, Spring 통합 테스트에서 `@ActiveProfiles("test")`로 활성화합니다.

운영 실행 시 `SPRING_PROFILES_ACTIVE=prod`와 `DB_URL`(JDBC URL), `DB_USERNAME`, `DB_PASSWORD` 환경 변수를 지정합니다.
다음 명령은 접근 가능한 PostgreSQL과 위 DB 환경 변수가 준비된 상태에서 실행합니다.

```bash
SPRING_PROFILES_ACTIVE=prod ./gradlew :board-service:launcher:bootRun
```

H2는 테스트 전용 의존성이며 운영 JAR에 포함되지 않습니다. 프로파일을 지정하지 않은 실행에는 별도 DB 설정이 필요합니다.
DB 통합 테스트는 H2 기반 검증과 Testcontainers PostgreSQL 기반 검증을 함께 수행합니다. PostgreSQL 테스트는 `prod` 설정으로 Flyway 적용·JPA 스키마 검증·저장 및 조회·앱 재기동 후 데이터 유지를 확인합니다.
`./gradlew test`와 `build` 실행에는 Docker가 필요하며, PostgreSQL 이미지가 없으면 최초 실행 시 내려받습니다. CI도 기본 테스트 경로에서 실행하며, 원격 실행 결과는 [PostgreSQL·Flyway 검증 기록](../records/postgres-flyway.md)에서 확인하고, 진행 상태는 [체크리스트](../checklist.md)에서 추적합니다.

컨테이너를 이용한 실행은 [배포와 실행 문서](README.md)를 참조한다.
