# Docker Compose로 실행

[compose.yaml](../../../compose.yaml)은 board-service와 PostgreSQL을 단일 Docker 호스트에서 실행하는 방식이다. 동일한 [앱 이미지](container-image.md)를 사용하며, Compose는 배포·실행 방식 중 하나로 제공한다. Kubernetes·ECS 등 다른 플랫폼을 선택하면 해당 플랫폼의 실행 정의를 별도로 구성한다.

모든 명령은 저장소 루트 기준이다. Docker Engine과 Docker Compose 플러그인이 필요하다. 현재 구성은 개발·실행 검증에 사용할 수 있으며, 운영 적용 전에는 아래 운영 항목을 확정해야 한다.

## 구성 파일

| 파일 | 역할 |
|---|---|
| [compose.yaml](../../../compose.yaml) | 앱 이미지·빌드 정의, DB 연결, 포트, 데이터 볼륨·healthcheck·재시작 정책 |
| [.env.example](../../../.env.example) | Compose 변수 목록과 안전한 예시. 실제 값은 `.env` 또는 실행 환경에서 제공 |
| [Dockerfile](../../../board-service/launcher/Dockerfile) | 소스 빌드 시 사용할 앱 이미지 정의 |

### 서비스와 데이터

- `board`: `prod` 프로파일로 실행한다. DB 주소는 Compose 네트워크의 서비스 이름 `postgres`를 사용하며 DB 계정은 실행 시 주입한다.
- `postgres`: 기존 통합 테스트와 맞춘 `postgres:17.6-alpine`을 사용한다. 호스트에 DB 포트를 공개하지 않는다.
- `depends_on: service_healthy`: PostgreSQL의 `pg_isready`가 성공한 뒤 앱을 시작한다. 이 검사는 DB 연결 수락 여부를 확인하며 앱·마이그레이션 준비 완료 검사는 아니다.
- `postgres-data`: DB를 보관하는 named volume이다. PostgreSQL 17의 데이터 경로 `/var/lib/postgresql/data`에 연결한다.
- 기본 네트워크·볼륨·컨테이너 이름은 Compose 프로젝트별로 생성된다. `container_name`은 고정하지 않는다.
- `restart: unless-stopped`: 종료된 프로세스는 다시 시작하되 사용자가 중지한 컨테이너는 자동 재시작하지 않는다. healthcheck 실패만으로 재시작하거나 DB 복구 후 앱 연결을 보장하지는 않는다.

## 환경 설정

```bash
cp .env.example .env
```

`.env`의 `POSTGRES_PASSWORD`를 직접 설정한다. 빈 값이면 Compose가 실행을 거부한다. `.env`는 Git과 Docker 빌드 컨텍스트에서 제외된다. Compose가 이 파일을 읽어 값을 치환하고, YAML의 `environment`에 지정한 값만 컨테이너로 전달한다. 같은 이름의 셸 환경 변수가 있으면 `.env` 값보다 우선한다.

| 변수 | 기본값 | 역할 |
|---|---|---|
| `POSTGRES_PASSWORD` | 없음, 필수 | DB 초기화와 앱 DB 인증에 사용 |
| `POSTGRES_DB` | `lilboard` | DB 이름 |
| `POSTGRES_USER` | `lilboard` | DB 초기화·앱 접속 계정 |
| `BOARD_IMAGE` | `lilboard-board:local` | 빌드 결과 태그 또는 실행할 기존 이미지·digest |
| `BOARD_BIND_ADDRESS` | `127.0.0.1` | 앱 포트를 바인딩할 호스트 주소 |
| `BOARD_PORT` | `8080` | 호스트에서 접근할 앱 포트. 컨테이너 포트는 8080 |

현재 DB 초기화 계정을 앱에서도 사용한다. 운영 계정 분리와 시크릿 저장소 연동은 후속 구성이다. 초기화 변수는 빈 볼륨에만 적용되므로 기존 볼륨의 비밀번호·DB 이름·사용자를 `.env` 변경만으로 바꿀 수 없다.

변수 치환과 구문은 다음 명령으로 확인한다. `--quiet` 없이 `config`를 실행하면 치환된 비밀번호가 출력될 수 있다.

```bash
docker compose config --quiet
```

## 소스에서 빌드하고 실행

```bash
docker compose up -d --build
```

저장소 루트를 컨텍스트로 앱 이미지를 빌드한다. JAR 빌드 과정에서 테스트는 실행하지 않으므로 별도 Gradle 검사 또는 CI를 통과해야 한다. DB가 healthy가 된 뒤 앱 컨테이너가 시작된다. 명령 성공 직후 앱 준비까지 완료된 것은 아니므로 아래 기동 확인 절차를 수행한다.

## 기존 이미지로 실행

`.env`의 `BOARD_IMAGE`를 접근 가능한 레지스트리의 태그 또는 `registry/path@sha256:...` 형태로 지정한다. 비공개 이미지는 해당 레지스트리 인증도 필요하다.

```bash
docker compose up -d --no-build --pull always
```

이 방식은 소스를 빌드하지 않고 지정한 이미지를 가져온다. 레지스트리 게시·인증 자동화는 아직 구성하지 않았다. 특정 결과물을 재현하려면 태그보다 digest로 식별한다.

## 기동 확인

```bash
docker compose ps
docker compose logs --tail 100 board
docker compose exec board id -u
docker compose exec postgres psql -U lilboard -d lilboard \
    -c 'SELECT version, success FROM flyway_schema_history;'
```

DB 이름·사용자를 변경했다면 `psql` 인자도 맞춘다. 기대값은 PostgreSQL healthy, 앱의 Tomcat 8080 기동 로그, UID `10001`, Flyway V1의 `success=t`다. 앱 기동 시 Flyway가 마이그레이션하고 JPA는 스키마를 검증한다.

기본 포트에서는 다음 요청이 HTTP 201과 생성 결과를 반환한다. 이 요청은 DB에 게시판을 추가한다.

```bash
curl -i -X POST http://127.0.0.1:8080/api/boards \
    -H 'Content-Type: application/json' \
    --data '{"name":"Compose 확인","description":"실행 검증"}'
```

## 재시작·변경 적용·종료

동일 설정의 앱 프로세스를 다시 시작한다.

```bash
docker compose restart board
```

이미지나 환경 변수 변경은 `restart`로 적용되지 않는다. 소스 변경은 `up -d --build`, 기존 이미지 갱신은 `up -d --no-build --pull always`로 적용한다.

컨테이너·네트워크를 제거하고 DB 볼륨은 유지한다. 다시 `up`하면 같은 프로젝트의 데이터를 재사용한다.

```bash
docker compose down
```

**DB 데이터까지 삭제해 초기화할 경우에만** 다음 명령을 사용한다.

```bash
docker compose down --volumes
```

프로젝트 이름은 디렉터리 이름을 기본으로 사용한다. `docker compose -p 이름 ...`으로 바꿀 수 있으며, 기동·확인·종료 때 같은 이름을 사용해야 동일한 볼륨과 컨테이너를 관리한다.

## 운영 적용 전 결정할 사항

현재 앱 포트는 기본적으로 호스트 loopback에만 공개된다. 운영 사용 시 TLS·리버스 프록시·접근 제어와 연결되는 바인딩 주소를 정한다. 또한 자원 제한, 앱 readiness와 장애 대응, 백업·복원, DB 최소 권한 계정·시크릿 교체, 이미지 digest 관리 및 배포·롤백 절차가 필요하다.

구성·실제 검증 상태는 [DevOps 체크리스트](../checklist.md)에, 운영 정책은 [ADR](../adr/)에 기록한다. Compose 파일 추가만으로 운영 배포 준비 완료를 의미하지 않는다.
