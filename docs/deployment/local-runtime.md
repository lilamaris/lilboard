# 로컬 실행 예시

[공통 컨테이너 이미지](container-image.md)를 PostgreSQL과 함께 실행하는 예시다. 모든 명령은 저장소 루트에서 실행하며 Docker Engine과 호스트의 빈 8080 포트가 필요하다. Dockerfile과 `.dockerignore`의 구성 설명은 공통 이미지 문서에서 관리한다.

현재 Compose·환경 변수 예시 파일·실행 스크립트는 구성 전이므로 Docker CLI로 실행한다. 해당 파일을 도입하면 이 문서도 갱신한다.

## 이미지 준비

```bash
docker build -f board-service/launcher/Dockerfile -t lilboard-board:local .
```

이 문서에서는 로컬 예시용 태그 `lilboard-board:local`을 사용한다.

## PostgreSQL과 앱 실행

아래 이름은 이 문서의 예시 전용이다. 비밀번호도 로컬 예시 값이며 원격 환경에는 별도 자격 증명을 사용한다. DB 포트는 호스트에 공개하지 않고 전용 Docker 네트워크로만 앱과 연결한다.

```bash
docker network create lilboard-local
docker volume create lilboard-local-pgdata
docker run -d --name lilboard-local-db \
    --network lilboard-local \
    -e POSTGRES_DB=lilboard \
    -e POSTGRES_USER=lilboard \
    -e POSTGRES_PASSWORD=local-example-only \
    -v lilboard-local-pgdata:/var/lib/postgresql/data \
    postgres:17.6-alpine
```

다음 명령이 종료 코드 0과 `accepting connections`를 반환할 때까지 기다린 뒤 앱을 실행한다.

```bash
docker exec lilboard-local-db pg_isready -U lilboard -d lilboard
```

```bash
docker run -d --name lilboard-local-board \
    --network lilboard-local \
    -p 127.0.0.1:8080:8080 \
    -e SPRING_PROFILES_ACTIVE=prod \
    -e DB_URL=jdbc:postgresql://lilboard-local-db:5432/lilboard \
    -e DB_USERNAME=lilboard \
    -e DB_PASSWORD=local-example-only \
    lilboard-board:local
```

`DB_URL`의 호스트는 DB 컨테이너 이름이다. 앱 컨테이너 내부의 `localhost`는 앱 자신을 가리킨다. 기존 외부 DB를 사용할 때는 이 URL과 계정 정보를 해당 DB에 맞게 바꾼다.

환경 변수와 Flyway·JPA 동작은 [공통 이미지 문서](container-image.md#실행-설정)를 참조한다.

이 구성에서는 앱 기동 시 Flyway가 마이그레이션을 수행하고 JPA는 스키마를 `validate`한다. 로컬 예시에서는 초기화 계정을 앱에서도 사용한다. 운영용 앱·마이그레이션 계정 권한 분리는 P2에서 구성한다. PostgreSQL 초기화 환경 변수는 빈 데이터 볼륨을 처음 초기화할 때 적용되며, 기존 볼륨의 비밀번호를 바꾸지는 않는다.

## 기동 확인과 재기동

```bash
docker logs --tail 100 lilboard-local-board
docker exec lilboard-local-board id -u
docker exec lilboard-local-db psql -U lilboard -d lilboard \
    -c 'SELECT version, success FROM flyway_schema_history;'
```

로그에서 Flyway 성공과 Tomcat 8080 기동을 확인한다. UID는 `10001`, Flyway V1 이력의 `success`는 `t`가 기대값이다. 로그 조회 명령 자체의 성공은 앱 준비 완료를 의미하지 않는다.

선택적으로 게시판 생성 요청으로 API를 확인할 수 있다. 성공 시 HTTP 201과 생성 결과를 반환하며 DB에 행을 추가한다.

```bash
curl -i -X POST http://localhost:8080/api/boards \
    -H 'Content-Type: application/json' \
    --data '{"name":"로컬 확인","description":"컨테이너 실행 확인"}'
```

설정과 이미지를 유지하고 앱만 재기동하려면 다음 명령을 사용한다. 환경 변수나 이미지를 변경하려면 기존 앱 컨테이너를 제거하고 새로 생성해야 한다.

```bash
docker restart lilboard-local-board
```

## 종료와 데이터 삭제

데이터를 유지하며 종료하고 컨테이너·네트워크만 제거한다.

```bash
docker stop lilboard-local-board lilboard-local-db
docker rm lilboard-local-board lilboard-local-db
docker network rm lilboard-local
```

이후 실행 절차를 다시 수행하면 같은 이름의 볼륨을 재사용한다. 완전히 초기화할 경우에만, DB 컨테이너를 제거한 후 아래 명령을 실행한다. **해당 로컬 볼륨의 DB 데이터가 모두 삭제된다.**

```bash
docker volume rm lilboard-local-pgdata
```

## 문제 확인

| 증상 | 확인할 내용 |
|---|---|
| 빌드 중 `port.out` 패키지를 찾지 못함 | `.dockerignore`에서 `src/main` 아래 `out` 보존 예외 확인 |
| 앱이 DB 연결 오류로 종료됨 | DB 준비 상태, 네트워크, JDBC 호스트 이름과 계정 확인 |
| DB 비밀번호를 바꿨는데 인증 실패 | 기존 볼륨에는 최초 비밀번호가 유지됨. DB 계정을 변경하거나, 데이터 삭제가 가능한 경우 볼륨 초기화 |
| 호스트 8080 바인딩 실패 | 호스트 포트 사용 여부 확인. `127.0.0.1:8081:8080`처럼 호스트 포트만 변경 가능 |
| 새 환경 변수·이미지가 적용되지 않음 | `docker restart` 대신 앱 컨테이너를 제거하고 다시 생성 |

현재 완료된 검증은 이미지 빌드, 외부 PostgreSQL 연결, Flyway V1·JPA 검증과 비루트 실행이다. 이 문서의 API 요청과 재기동 데이터 유지 절차는 사용 예시이며, 실제 수행 여부는 체크리스트의 검증 기록과 구분한다.
