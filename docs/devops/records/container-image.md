# 컨테이너 이미지 검증

[DevOps 체크리스트](../checklist.md)의 상세 설정과 검증 근거를 기록한다. 날짜·커밋·실행 결과는 기존 확인 시점의 기록이며, 문서 분리 과정에서 원격 설정이나 런타임을 재검증하지 않았다.

## Dockerfile

대상: `Dockerfile`(multi-stage 또는 layered jar, 비루트 사용자, OCI 레이블)

`board-service/launcher/Dockerfile`: Java 21 JDK 빌드 → JRE 런타임, UID/GID 10001, OCI title·source 레이블(`21291db`). 이미지 빌드 및 외부 PostgreSQL 연결·prod 기동 확인(2026-10-23). [로컬 이미지 검증 기록](#로컬-이미지-검증-기록) 참조

## Docker 빌드 컨텍스트

대상: `.dockerignore`

루트에 Git·IDE·Gradle 캐시·빌드 산출물·환경 파일·키 파일 제외 설정 추가(`21291db`). 임시 검증 이미지에서 Git·캐시·산출물 제외 및 wrapper·`port/out` 소스 보존 확인(2026-10-23). [로컬 이미지 검증 기록](#로컬-이미지-검증-기록) 참조

## 이미지 게시 후속 작업

대상: 이미지 빌드·push와 git SHA·릴리스 태그 정책

Dockerfile 수동 빌드는 검증 완료. CI 이미지 빌드·push, 소스 SHA·이미지 digest 추적 및 `0.0.1-SNAPSHOT` 릴리스 태그 정책은 미구성

## 로컬 이미지 검증 기록

- 대상: `2aa70e5` 기반 `feat/p1-runtime-environment`의 미커밋 변경, 2026-10-23(Asia/Seoul).
- 저장소 루트를 빌드 컨텍스트로 사용한다. Dockerfile은 서비스의 launcher 모듈에 둔다.
- 빌드: `docker build -f board-service/launcher/Dockerfile -t lilboard-board:local .`
- 런타임 설정: `SPRING_PROFILES_ACTIVE=prod`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`를 실행 시 주입한다. 이미지에 접속 정보를 저장하지 않는다. 포트는 8080이며 JVM 옵션은 `JAVA_TOOL_OPTIONS`로 전달할 수 있다.
- 이미지 빌드는 `:board-service:launcher:bootJar`만 실행한다. 테스트·커버리지·포맷 검사는 CI 또는 별도 Gradle 명령으로 실행하며 이미지 빌드 성공과 구분한다.
- 컨텍스트 검증: 동일 루트 컨텍스트를 복사한 임시 이미지에서 `.git`·`.gradle`·`.aws`·launcher/build·build-logic/convention/build 부재, Gradle wrapper JAR·`port/out/BoardSavePort.java` 존재 확인. `out`·`build`·`bin` 제외 패턴에는 `src/main`·`src/test` 하위 소스 예외를 적용했다.
- 이미지 빌드 성공: `lilboard-board:local`, image config ID `sha256:aed1860b61c5d455479dce8b3b18c5f31592af9d81b0426fb1e18b77f5d183bb`.
- 기동 검증: 임시 Docker 네트워크의 `postgres:17.6-alpine`에 `prod` 프로파일과 `DB_*`를 전달해 앱 기동 성공. Tomcat 8080 기동, Flyway V1 성공 이력 1건, JPA `validate` 통과, 실행 UID 10001 확인. 검증 후 임시 앱·DB 컨테이너와 DB 볼륨·네트워크 정리.
- `./gradlew spotlessApply spotlessCheck --console=plain` 및 `git diff --check` 통과. Java 동작 변경이 없어 단위 테스트·전체 빌드는 별도 실행하지 않았다.
- 당시 이미지 검증에서는 Compose·API·재기동을 실행하지 않았다. 이후 Compose로 수행한 API·데이터 유지 검증은 [Compose 검증 기록](compose.md#compose-검증-기록)을 참조한다. 원격 이미지 게시 및 이미지 빌드 CI는 아직 미구성이다.

- 사용 문서 추가(2026-10-23): [배포와 실행](../deployment/README.md)에 공통 이미지 구성 문서와 로컬 실행 예시를 나누어 정리했다. Dockerfile·`.dockerignore` 설명은 환경 공통으로 관리하며, 수동 기동·종료·데이터 삭제 예시는 로컬 실행 문서에서 관리한다. 문서의 명령은 기존 기동 검증 결과와 대조하고 셸 구문·상대 링크·포맷을 검사했다. 이번 문서 작업에서 컨테이너·API·재기동 검증은 재실행하지 않았다.
