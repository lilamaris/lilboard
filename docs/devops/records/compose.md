# Compose 실행 환경 검증

[DevOps 체크리스트](../checklist.md)의 상세 설정과 검증 근거를 기록한다. 날짜·커밋·실행 결과는 기존 확인 시점의 기록이며, 문서 분리 과정에서 원격 설정이나 런타임을 재검증하지 않았다.

## Compose 구성

대상: `compose.yaml`(PostgreSQL + healthcheck, 앱의 `depends_on: service_healthy`)

앱·PostgreSQL 구성, 소스 빌드·기존 이미지 선택, DB healthcheck·볼륨·재시작 정책. 새 볼륨 기동·API·앱 재시작·down/up 데이터 유지 확인(2026-10-23). [Compose 검증 기록](#compose-검증-기록) 참조

## 환경 변수 예시

대상: `.env.example`

DB·이미지·바인딩 주소·포트 설명 및 예시 제공. 비밀번호는 빈 값이며 미설정 시 Compose 실패 확인. `.env` Git 제외·예시 추적 허용 확인(2026-10-23)

## 운영 적용 범위

대상: 운영 런타임 정의(Compose/VM, Kubernetes manifest/Helm 또는 ECS task definition)

Compose 실행·데이터 유지는 로컬 검증 완료. 실제 운영 대상 확정, 리소스·네트워크·시크릿 등 운영 설정 및 해당 환경의 기동 검증은 미완료

## Compose 검증 기록

- 대상: `21291db` 기반 `feat/p1-runtime-environment`의 미커밋 변경, 2026-10-23(Asia/Seoul). 사용 방법은 [Docker Compose 문서](../deployment/compose.md)를 참조한다.
- 구성: 앱 `build`·`image` 병행, DB `postgres:17.6-alpine`, `service_healthy` 의존, named volume, `unless-stopped` 재시작. DB 호스트 포트 비공개, 앱은 기본 loopback 바인딩.
- 환경: Docker Compose 5.6.0. 기존 검증 이미지 `lilboard-board:local` 사용. 실제 `.env` 대신 임시 셸 환경 변수를 주입하고 `--env-file /dev/null`로 사용자 설정을 배제했다.
- 설정 검사: `docker compose ... config --quiet`에 해당하는 검사에서 빈 비밀번호 실패 확인, JSON 렌더링으로 DB healthcheck 의존 및 DB 호스트 포트 미공개 확인. `git check-ignore --no-index`로 `.env` 제외·`.env.example` 허용 확인.
- 실행: 별도 프로젝트 `lilboard-p1-check`와 임시 호스트 포트로 `up -d --no-build --wait --wait-timeout 90`, HTTP 준비 상태 별도 대기, `POST /api/boards`의 HTTP 201·ID·DB 저장 1건 확인. 실행 UID 10001 확인.
- 재기동: `restart board` 후 저장 행 1건 유지. `down` 후 같은 프로젝트로 다시 `up`하여 데이터 1건 유지 및 Flyway 성공 이력 1건 확인.
- 정리: 검증 프로젝트에만 `down --volumes`를 실행해 임시 컨테이너·네트워크·볼륨 제거.
- 문서 셸 구문 20개·상대 링크·`git diff --check`·Spotless 검사 통과. Java 동작 변경이 없어 단위 테스트·전체 빌드는 재실행하지 않았다.
- 미확인: 이번 작업에서 Compose의 소스 재빌드·레지스트리 pull·원격 CI·운영 배포는 실행하지 않았다. 소스 이미지 빌드 자체의 성공 근거는 [로컬 이미지 검증 기록](container-image.md#로컬-이미지-검증-기록)에 있다. Compose 구성 완료는 운영 배포 준비 완료와 구분한다.

- 실행 안내 정리(2026-10-23): 루트 README는 Compose·Docker CLI·JVM 실행 방법과 상세 문서 링크만 제공한다. 기존 프로파일·`bootRun` 설명은 [JVM 실행 문서](../deployment/jvm-runtime.md)로 이동하고, 빌드·테스트의 Docker 요구사항은 README 빌드 섹션에 유지했다.
