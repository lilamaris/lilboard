# lilboard

MSA(Microservices Architecture) 기반 게시판 서비스를 목표로 하는 Java·Gradle 멀티 모듈 프로젝트입니다.

## 목차

- [프로젝트 구조](#프로젝트-구조)
- [개발 환경](#개발-환경)
- [빌드 및 검증](#빌드-및-검증)
- [실행](#실행)
- [게시판 API](#게시판-api)
- [에이전트 작업 규칙](#에이전트-작업-규칙)
- [DevOps 진행 상태](#devops-진행-상태)

## 프로젝트 구조

```text
board-service/
  domain/       # 게시판 도메인
  application/  # 애플리케이션 계층
  jpa/          # DB 영속성 어댑터
  web/          # HTTP API 어댑터
  launcher/     # 실행 및 환경 설정
kernel/         # 공통 코드
build-logic/    # 공통 빌드·포맷 설정
docs/           # 배포·실행 문서, DevOps 체크리스트, ADR
```

## 개발 환경

- JDK 21
- Gradle Wrapper 사용 (`gradlew`, `gradlew.bat`)
- `gradle.properties`에 캐시·configuration cache·parallel 실행 설정

## 빌드 및 검증

프로젝트 루트에서 실행합니다. `test`와 `build`에는 PostgreSQL 통합 테스트를 위한 Docker가 필요하며, 최초 실행 시 테스트 이미지를 내려받을 수 있습니다.

```bash
./gradlew build          # 포맷 검사·빌드·테스트·커버리지 게이트
./gradlew spotlessApply  # 자동 포맷
./gradlew spotlessCheck  # 포맷 검사
./gradlew jacocoTestReport  # 테스트 실행 및 모듈별 커버리지 보고서 생성
```

`check` 단계에서 모듈별 라인 커버리지 게이트(기본 80%)를 검사합니다.
현재 실행 진입점 모듈(`launcher`)은 `coverage.excludes=*launcher*`로 번들 단위 검사에서 제외합니다.
기준은 `coverage.minimum`·`coverage.excludes`로 조정합니다. [launcher 제외 정책](docs/adr/0001-launcher-coverage-policy.md)을 확정하고 게이트 실패·제외 동작을 검증했으며, launcher 역할 제한을 위한 ArchUnit 검증은 P0 작업으로 남아 있습니다.
아키텍처 경계(domain이 상위 계층·Spring에 의존하지 않음, application이 어댑터에 의존하지 않음, kernel이 board-service에 의존하지 않음)는 ArchUnit 테스트로 검증합니다.

`main` 브랜치 push와 PR, 수동 실행에서 GitHub Actions로 포맷 검사(lint job)를 수행하고, 통과 후 빌드·테스트(build job)를 수행합니다.
테스트 후 각 모듈의 `build/reports/jacoco/test/`에 HTML·XML 보고서를 생성하며,
CI에서는 테스트·커버리지 보고서를 artifact로 7일간 보관합니다.

Dependabot은 매주 월요일 오전 9시(한국 시간)에 Gradle·GitHub Actions 업데이트를 확인합니다.
minor·patch 업데이트는 묶고, major 업데이트는 개별 PR로 생성합니다.

## 실행

| 방법 | 설명 | 문서 |
|---|---|---|
| Docker Compose | 앱과 PostgreSQL을 함께 실행. 소스 빌드 또는 기존 이미지 사용 | [Compose 실행](docs/deployment/compose.md) |
| Docker CLI | 앱 이미지와 PostgreSQL 컨테이너를 직접 실행·관리 | [컨테이너 실행 예시](docs/deployment/local-runtime.md) |
| JVM | 준비된 PostgreSQL에 연결하여 Gradle `bootRun`으로 실행 | [JVM 실행](docs/deployment/jvm-runtime.md) |

이미지 구성·빌드와 실행 설정은 [배포와 실행 문서](docs/deployment/README.md)에서 안내합니다.

## 게시판 API

애플리케이션 기동 후 `POST /api/boards`로 게시판을 생성합니다.

```bash
curl -i -X POST http://localhost:8080/api/boards \
  -H 'Content-Type: application/json' \
  -d '{"name":"자유게시판","description":"자유롭게 이야기를 나눠요"}'
```

성공 시 `201 Created`와 `id`, `name`, `description`, `createdAt`을 반환합니다.
이름과 설명은 필수이며 공백만 입력할 수 없습니다. 이름은 최대 50자, 설명은 최대 300자이고, 잘못된 입력은 `400 Bad Request`를 반환합니다.
launcher HTTP 통합 테스트는 H2에서 API → 유스케이스 → JPA 저장 경로를 검증합니다.

## 에이전트 작업 규칙

[AGENTS.md](AGENTS.md)는 AI 에이전트가 이 저장소에서 작업할 때 따르는 지침입니다.
코드 변경, 포맷, 테스트 및 커밋 규칙을 정의합니다.

## DevOps 진행 상태

현재 CI·포맷·테스트·커버리지 게이트·아키텍처 검증과 DB 프로파일 분리가 구성되어 있습니다.
컨테이너 이미지 빌드와 Compose 실행 구성이 있으며, 이미지 기동·API·재기동 데이터 유지를 로컬에서 검증했습니다.
현재 CI는 Gradle 빌드·테스트를 수행하며, 컨테이너 이미지 빌드·게시·Compose 실행은 자동화되어 있지 않습니다. 원격 배포와 운영 관찰성·백업·복구는 후속 작업입니다.

[docs/devops-checklist.md](docs/devops-checklist.md)에서 완료 항목, 작업별 성공 조건, 결정 필요 사항과 검증 메모를 관리합니다.

- P0: 품질·보안 기준과 운영 DB 선행 검증
- P1: 이미지와 실행·인증 환경 준비
- P2: 첫 운영 배포 전 배포·복구·관찰성 구성
- P3: 협업 편의와 확장 최적화

DevOps 변경 시 체크리스트 상태를 함께 갱신하고, 완료 항목에는 커밋 해시와 검증 근거를 기록합니다.
GitHub 설정은 적용 대상·확인 일자와 실제 강제 동작을 확인하며, 배포 대상·레지스트리·시크릿 관리 방식은 이미지 게시·배포 인증 등 해당 결정에 의존하는 구성 전에 확정합니다.
