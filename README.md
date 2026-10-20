# lilboard

MSA(Microservices Architecture) 기반 게시판 서비스를 목표로 하는 Java·Gradle 멀티 모듈 프로젝트입니다.

## 목차

- [프로젝트 구조](#프로젝트-구조)
- [개발 환경](#개발-환경)
- [빌드 및 검증](#빌드-및-검증)
- [실행 프로파일](#실행-프로파일)
- [에이전트 작업 규칙](#에이전트-작업-규칙)
- [DevOps 진행 상태](#devops-진행-상태)

## 프로젝트 구조

```text
board-service/
  domain/       # 게시판 도메인
  application/  # 애플리케이션 계층
  jpa/          # DB 영속성 어댑터
  launcher/     # 실행 및 환경 설정
kernel/         # 공통 코드
build-logic/    # 공통 빌드·포맷 설정
docs/           # DevOps 체크리스트 등 프로젝트 문서
```

## 개발 환경

- JDK 21
- Gradle Wrapper 사용 (`gradlew`, `gradlew.bat`)
- `gradle.properties`에 캐시·configuration cache·parallel 실행 설정

## 빌드 및 검증

프로젝트 루트에서 실행합니다.

```bash
./gradlew build          # 포맷 검사·빌드·테스트·커버리지 게이트
./gradlew spotlessApply  # 자동 포맷
./gradlew spotlessCheck  # 포맷 검사
./gradlew jacocoTestReport  # 테스트 실행 및 모듈별 커버리지 보고서 생성
```

`check` 단계에서 모듈별 라인 커버리지 게이트(기본 80%)를 검사합니다.
현재 실행 진입점 모듈(`launcher`)은 `coverage.excludes=*launcher*`로 번들 단위 검사에서 제외합니다.
기준은 `coverage.minimum`·`coverage.excludes`로 조정하며, launcher 예외 정책 확정과 게이트 실패·제외 동작 검증은 체크리스트의 P0 항목으로 남아 있습니다.
아키텍처 경계(domain이 상위 계층·Spring에 의존하지 않음, application이 어댑터에 의존하지 않음, kernel이 board-service에 의존하지 않음)는 ArchUnit 테스트로 검증합니다.

`main` 브랜치 push와 PR, 수동 실행에서 GitHub Actions로 포맷 검사(lint job)를 수행하고, 통과 후 빌드·테스트(build job)를 수행합니다.
테스트 후 각 모듈의 `build/reports/jacoco/test/`에 HTML·XML 보고서를 생성하며,
CI에서는 테스트·커버리지 보고서를 artifact로 7일간 보관합니다.

Dependabot은 매주 월요일 오전 9시(한국 시간)에 Gradle·GitHub Actions 업데이트를 확인합니다.
minor·patch 업데이트는 묶고, major 업데이트는 개별 PR로 생성합니다.

## 실행 프로파일

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
현재 DB 통합 테스트는 H2 기반이며, 실제 PostgreSQL에서 Flyway 적용·JPA 스키마 검증·저장 및 조회를 확인하는 Testcontainers 테스트는 P0 작업으로 남아 있습니다.

## 에이전트 작업 규칙

[AGENTS.md](AGENTS.md)는 AI 에이전트가 이 저장소에서 작업할 때 따르는 지침입니다.
코드 변경, 포맷, 테스트 및 커밋 규칙을 정의합니다.

## DevOps 진행 상태

현재 CI·포맷·테스트·커버리지 게이트·아키텍처 검증과 DB 프로파일 분리가 구성되어 있습니다.
컨테이너 이미지, 배포 자동화, 운영 관찰성·백업·복구는 후속 작업입니다.

[docs/devops-checklist.md](docs/devops-checklist.md)에서 완료 항목, 작업별 성공 조건, 결정 필요 사항과 검증 메모를 관리합니다.

- P0: 품질·보안 기준과 운영 DB 선행 검증
- P1: 이미지와 실행·인증 환경 준비
- P2: 첫 운영 배포 전 배포·복구·관찰성 구성
- P3: 협업 편의와 확장 최적화

DevOps 변경 시 체크리스트 상태를 함께 갱신하고, 완료 항목에는 커밋 해시와 검증 근거를 기록합니다.
GitHub 설정은 적용 대상·확인 일자와 실제 강제 동작을 확인하며, 배포 대상·레지스트리·시크릿 관리 방식은 관련 P1 구성 전에 결정합니다.
