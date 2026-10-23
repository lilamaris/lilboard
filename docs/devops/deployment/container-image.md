# 컨테이너 이미지 구성과 빌드

서비스 이미지의 빌드·실행에 공통으로 사용하는 구성을 설명한다. Dockerfile과 `.dockerignore`는 로컬과 CI에서 공통으로 사용할 수 있으며, 현재 CI의 이미지 빌드는 아직 구성하지 않았다. 실행 시 환경별 DB 설정과 시크릿을 주입한다. 모든 명령은 저장소 루트 기준이다.

| 파일 | 역할 |
|---|---|
| [board-service/launcher/Dockerfile](../../../board-service/launcher/Dockerfile) | board-service 실행 JAR 빌드 및 실행 이미지 생성 |
| [.dockerignore](../../../.dockerignore) | 빌드 컨텍스트에서 불필요한 파일 제외 |
| [application-prod.yml](../../../board-service/launcher/src/main/resources/application-prod.yml) | 외부 PostgreSQL 접속·Flyway·JPA 검증 설정 |

## 사전 준비

- Docker CLI와 실행 중인 Docker Engine이 필요하다.
- 이미지와 Gradle 배포 파일·의존성을 내려받기 위한 네트워크 접근이 필요하다.
- 이미지 빌드에는 호스트의 Java·Gradle 설치가 필요하지 않다. 별도 Gradle 검사에는 Java 21이 필요하다.

```bash
docker info --format '{{.ServerVersion}}'
```

## 사용하는 이미지

| 용도 | 이미지 | 설명 |
|---|---|---|
| 앱 빌드 | `eclipse-temurin:21-jdk-jammy` | Java 21 JDK와 Ubuntu 22.04 기반 환경에서 Gradle wrapper 실행 |
| 앱 실행 | `eclipse-temurin:21-jre-jammy` | 동일 Java 계열의 JRE로 실행 JAR 구동 |

현재 베이스 이미지는 태그로 지정되어 있어 이후 빌드 시 같은 태그가 다른 digest를 가리킬 수 있다. 원격 게시 시 소스 SHA·이미지 digest·릴리스 태그 관리 방식은 별도 결정한다. `lilboard-board:example`은 빌드 명령을 설명하기 위한 태그이며 실제 게시 태그는 후속 정책에 따른다.

## Dockerfile 구성

Dockerfile은 실행 앱을 조립하는 launcher 모듈에 둔다. 빌드 컨텍스트는 공통 모듈 `kernel`, `build-logic`, Gradle wrapper 등을 포함해야 하므로 저장소 루트를 사용한다.

### build 단계

1. Java 21 JDK 이미지에서 `/workspace`를 작업 디렉터리로 지정한다.
2. `.dockerignore`가 적용된 저장소 파일을 복사한다.
3. `gradlew`에 실행 권한을 부여하고 `:board-service:launcher:bootJar`를 실행한다.
4. 생성한 `*-SNAPSHOT.jar`를 `/workspace/app.jar`로 복사한다. 현재 버전 `0.0.1-SNAPSHOT`을 전제로 하므로 릴리스 버전 도입 시 이 선택 패턴도 변경해야 한다.

이 단계는 실행 JAR만 만든다. 단위·통합 테스트, 커버리지, 포맷 검사는 별도 Gradle 검사 또는 CI에서 실행한다.

### runtime 단계

- JRE 이미지에 `app` 그룹·사용자를 UID/GID `10001`로 생성한다.
- build 단계의 JAR만 `/app/app.jar`로 복사하고 소유자를 설정한다. 빌드 단계의 소스·Gradle 캐시는 최종 이미지로 복사하지 않는다.
- `USER 10001:10001`로 비루트 실행한다.
- OCI 레이블에 이미지 제목과 소스 저장소를 기록한다.
- `EXPOSE 8080`으로 컨테이너의 앱 포트를 명시한다. 호스트 포트 공개는 Docker 실행 시 `-p` 또는 배포 플랫폼의 네트워크 설정으로 별도 지정한다.
- `ENTRYPOINT`는 `java -jar /app/app.jar`다. JVM 옵션은 `JAVA_TOOL_OPTIONS`로 전달할 수 있다.

## .dockerignore 구성

Git 메타데이터, GitHub 설정, 에이전트·IDE 설정, Gradle 캐시, 로컬 빌드 산출물, 로그 및 일반적인 자격 증명 파일 패턴을 제외한다. `.env`·`.env.*`는 제외하고 `.env.example`은 안전한 예시 파일을 위한 예외로 둔다.

`out`, `build`, `bin`은 산출물 경로와 Java 패키지 이름 모두에 쓰일 수 있다. 따라서 `src/main`·`src/test` 하위에는 보존 예외를 둔다. 특히 application의 `port/out`은 소스 패키지이므로 제외하면 이미지 안에서 컴파일이 실패한다. Gradle wrapper JAR도 빌드에 필요하므로 보존한다.

`.dockerignore`는 Docker 컨텍스트만 제어한다. Git 추적 여부는 `.gitignore`에서 별도로 관리한다. 새 자격 증명 파일 형식을 도입하면 해당 파일의 제외 여부도 확인한다.

## 이미지 빌드

```bash
docker build -f board-service/launcher/Dockerfile -t lilboard-board:example .
```

마지막 `.`이 저장소 루트 컨텍스트다. launcher 디렉터리를 컨텍스트로 지정하면 공통 모듈과 Gradle 설정이 누락된다. 현재는 소스 전체를 복사한 다음 빌드하므로 컨텍스트 파일이 변경되면 Gradle 실행 레이어도 다시 빌드될 수 있다.

## 실행 설정

| 환경 변수 | 역할 |
|---|---|
| `SPRING_PROFILES_ACTIVE=prod` | PostgreSQL·Flyway 설정 활성화 |
| `DB_URL` | PostgreSQL JDBC URL |
| `DB_USERNAME`, `DB_PASSWORD` | 앱의 DB 접속 계정 |
| `JAVA_TOOL_OPTIONS` | 선택 사항. 예: `-Xmx512m`과 같은 JVM 옵션 |

앱 기동 시 Flyway가 마이그레이션을 수행하고 JPA는 스키마를 `validate`한다. 접속 정보는 이미지를 만들 때 저장하지 않고 실행 환경에서 주입한다. 원격 환경의 시크릿 저장소·주입 방식과 앱·마이그레이션 계정 분리는 별도 결정한다.

실행 방법은 [Docker Compose](compose.md) 또는 [Docker CLI 예시](local-runtime.md)를 참조한다. 레지스트리 게시, 배포 플랫폼별 런타임 정의·인증·네트워크 설정은 아직 구성하지 않았다. 이미지 빌드·기동과 Compose API·데이터 유지의 검증 범위는 [이미지 검증 기록](../records/container-image.md)과 [Compose 검증 기록](../records/compose.md)에 각각 기록한다. 진행 상태는 [DevOps 체크리스트](../checklist.md)에서 관리한다.
