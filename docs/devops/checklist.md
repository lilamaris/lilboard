# DevOps 체크리스트

[DevOps 문서 안내](README.md)에서 문서 구조와 갱신 방법을 확인한다. 이 문서는 항목별 상태·완료 기준·상세 근거 링크를 관리한다. 설정 값·확인 일자·커밋·실행 이력은 `records/`의 항목별 문서에 기록한다.

파일 작성만으로 완료 처리하지 않고 구성 완료와 실제 동작 확인을 구분한다. 상태는 각 상세 기록의 확인 시점을 기준으로 하며, 문서 분리로 기존 항목의 완료 여부를 바꾸지 않았다.

## 우선순위

| 구분 | 내용 |
|---|---|
| P0 | 다음 단계 진행 전에 확인할 품질·보안 기준과 운영 DB 검증 |
| P1 | 배포 가능한 이미지와 반복 가능한 개발·인증 환경 준비 |
| P2 | 첫 운영 배포 전 갖출 배포·복구·관찰성 구성 |
| P3 | 협업 편의와 서비스 확장에 따른 최적화 |

P0를 우선 진행하되, 독립적인 P1 공통 컨테이너 구성과 로컬 실행 검증은 병행할 수 있다. 배포 대상·레지스트리·시크릿 관리 방식은 이미지 게시·배포 인증 등 해당 결정에 의존하는 작업 전에 확정한다.
플랫폼 설정 활성화와 실제 차단 동작 검증은 별도 항목으로 관리한다. 플랫폼 상태는 항목별 상세 문서의 확인 시점을 기준으로 한다.

## 완료

| 상태 | 항목 | 완료 기준·상세 근거 |
|:---:|---|---|
| ☑ | Spotless 포맷 검사 + `.editorconfig`·`.gitattributes` (BUILD 단계에 포함) | [설정·검증 기록](records/ci-and-quality.md#포맷-검사) |
| ☑ | Dependabot(gradle·github-actions, weekly, minor·patch 그룹) | [설정·검증 기록](records/dependencies.md#버전-업데이트) |
| ☑ | JaCoCo 커버리지 보고서(html·xml)와 CI artifact 보관(7일) | [설정·검증 기록](records/coverage.md#보고서와-보관) |
| ☑ | 실행 프로파일 분리(`prod` PostgreSQL + Flyway / `test` H2) | [설정·검증 기록](records/postgres-flyway.md#실행-프로파일) |
| ☑ | Gradle 실행 설정: 캐시·configuration cache·parallel·jvmargs (`gradle.properties`) | [설정·검증 기록](records/ci-and-quality.md#gradle-실행-설정) |
| ☑ | 모듈별 라인 커버리지 게이트(기본 80%, `coverage.minimum`·`coverage.excludes`) | [설정·검증 기록](records/coverage.md#모듈별-게이트) |
| ☑ | ArchUnit 계층 경계 검증(domain → 상위 계층·Spring 금지, application → 어댑터 금지, kernel → board-service 금지) | [설정·검증 기록](records/ci-and-quality.md#계층-경계) |
| ☑ | CI 트리거(main push + PR + 수동 실행)와 lint job 분리 | [설정·검증 기록](records/ci-and-quality.md#ci-실행-경로) |

## P0: 선행 검증과 저장소 보호

| 상태 | 항목 | 완료 기준·상세 근거 |
|:---:|---|---|
| ☑ | GitHub 보안 기능 사용 가능 여부 확인 | [설정·검증 기록](records/github-security.md#사용-가능-범위) |
| ☑ | Secret scanning + push protection 활성화 | [설정·검증 기록](records/github-security.md#secret-scanning과-push-protection-활성화) |
| ☐ | Push protection 실제 차단 검증 | [설정·검증 기록](records/github-security.md#push-protection-차단-검증) |
| ☑ | CodeQL default setup 적용(파일 없음, 2026-10-21) | [설정·검증 기록](records/github-security.md#codeql-설정) |
| ☑ | CodeQL 첫 스캔 결과 확인(2026-10-21) | [설정·검증 기록](records/github-security.md#codeql-실행-결과) |
| ☑ | Dependabot alerts 활성화(2026-10-21, 사용자 UI 작업) | [설정·검증 기록](records/dependencies.md#보안-알림) |
| ☑ | Dependabot automated security updates 활성화(2026-10-21, 사용자 UI) | [설정·검증 기록](records/dependencies.md#보안-업데이트) |
| ☐ | dependency graph에 Gradle·Java 의존성 포함 확인 | [설정·검증 기록](records/dependencies.md#의존성-그래프) |
| ☑ | rulesets 적용(2026-10-21) | [설정·검증 기록](records/repository-protection.md#ruleset-설정) |
| ☑ | rulesets 실제 병합 차단 확인(2026-10-21) | [설정·검증 기록](records/repository-protection.md#병합-차단-검증) |
| ☐ | CODEOWNERS 파일 + `require_code_owner_review` | [설정·검증 기록](records/repository-protection.md#코드-소유자-승인) |
| ☐ | dependency review 차단 정책 | [설정·검증 기록](records/dependencies.md#dependency-review) |
| ☑ | launcher 커버리지 예외 정책 확정 | [설정·검증 기록](records/coverage.md#launcher-제외-정책) |
| ☑ | 커버리지 게이트 실패·제외 동작 검증 | [설정·검증 기록](records/coverage.md#게이트-동작-검증) |
| ☐ | launcher 역할 제한 ArchUnit 검증 | [설정·검증 기록](records/coverage.md#launcher-역할-제한) |
| ☑ | Testcontainers PostgreSQL 기반 Flyway 통합 테스트 | [설정·검증 기록](records/postgres-flyway.md#통합-테스트-구성) |
| ☑ | 통합 테스트 CI 실행 경로 구성 | [설정·검증 기록](records/postgres-flyway.md#통합-테스트-ci-경로) |
| ☑ | PostgreSQL 통합 테스트 원격 CI 결과 확인 | [설정·검증 기록](records/postgres-flyway.md#원격-ci-결과) |

## P1: 이미지와 실행 환경 준비

파일 구성·이미지 설명·Compose·Docker CLI·JVM 실행 방법은 [배포와 실행](deployment/README.md)을 참조한다.
로컬 검증과 원격 CI·운영 배포 확인을 구분한다.

| 상태 | 항목 | 완료 기준·상세 근거 |
|:---:|---|---|
| ☐ | 배포 대상·레지스트리·Runner·시크릿 관리 방식 결정 | 아래 결정 항목을 ADR에 기록한 뒤 관련 구성 진행 |
| ☑ | `Dockerfile`(multi-stage 또는 layered jar, 비루트 사용자, OCI 레이블) | [설정·검증 기록](records/container-image.md#dockerfile) |
| ☑ | `.dockerignore` | [설정·검증 기록](records/container-image.md#docker-빌드-컨텍스트) |
| ☑ | `compose.yaml`(PostgreSQL + healthcheck, 앱의 `depends_on: service_healthy`) | [설정·검증 기록](records/compose.md#compose-구성) |
| ☑ | `.env.example` | [설정·검증 기록](records/compose.md#환경-변수-예시) |
| ☐ | `scripts/` | 문서화된 명령으로 로컬 기동·종료·검증을 재현하고 데이터 삭제 명령은 구분 |
| ☐ | 이미지 빌드·push와 git SHA·릴리스 태그 정책 | [설정·검증 기록](records/container-image.md#이미지-게시-후속-작업) |
| ☐ | SBOM 생성(cyclonedx 또는 syft) | 해당 이미지 digest와 연결해 보관 |
| ☐ | 이미지 스캔(Trivy 등) | 차단 심각도와 예외 승인·만료 정책을 정하고 취약 이미지 차단 확인 |
| ☐ | GitHub Environments(dev/staging/prod)와 배포 인증 | 클라우드 사용 시 OIDC 신뢰 조건을 저장소·환경에 한정하고 최소 권한 확인 |
| ☐ | `SECURITY.md` | 취약점 신고 경로와 지원 범위 명시 |
| ☐ | `LICENSE` | 배포·공개 범위에 맞는 라이선스를 결정하고 파일 반영 |

## P2: 첫 운영 배포 전 필수 구성

| 상태 | 항목 | 완료 기준·상세 근거 |
|:---:|---|---|
| ☐ | 운영 런타임 정의(Compose/VM, Kubernetes manifest/Helm 또는 ECS task definition) | [설정·검증 기록](records/compose.md#운영-적용-범위) |
| ☐ | probe(liveness/readiness)와 종료 정책 | 트래픽 수신 조건과 프로세스 재시작 조건을 분리하고 종료 유예 시간 내 요청 처리 확인 |
| ☐ | HikariCP 풀·연결 타임아웃 | DB 연결 한도와 장애 시 응답 시간을 고려해 설정하고 연결 실패 동작 확인 |
| ☐ | 배포 워크플로(rollout → readiness/스모크 테스트 → 실패 시 앱 rollback) | 실패 배포를 주입해 이전 이미지 digest 복구 확인 |
| ☐ | 이미지 승격 정책 | staging에서 검증한 동일 digest를 prod에 배포하고 환경마다 다시 빌드하지 않음 |
| ☐ | 배포 동시 실행 제한과 운영 승인 | 환경별 직렬화와 진행 중 배포 취소 정책을 정하고 승인·실패 처리 확인 |
| ☐ | DB 마이그레이션 실행 주체·시점·실패 대응 | 이전 앱 버전과 호환되는 스키마 변경 원칙을 정하고 앱 rollback과 DB 복구 절차를 분리 |
| ☐ | DB 백업·복원 | 보관 기간·암호화·접근 권한과 목표 RPO·RTO를 정하고 별도 환경에서 실제 복원 검증 |
| ☐ | TLS·도메인·네트워크 접근 제어 | 인증서 갱신과 외부 노출 범위를 정하고 DB 외부 접근 제한 확인 |
| ☐ | DB 최소 권한 계정과 시크릿 수명 관리 | 앱·마이그레이션 권한을 구분하고 자격 증명 주입·교체 절차 확인 |
| ☐ | 관찰성 연결(actuator + Prometheus + OTel) | [앱 측 구성·검증 기록](records/observability.md). 카탈로그 선언과 실제 연결을 구분하고, 헬스·메트릭·trace 수집 및 관리 엔드포인트 접근 제한 확인. 앱 로그의 OTLP 전송과 원격 관측성 호스트 수집은 미확인 |
| ☐ | JSON 구조화 로그와 trace 전파 | 요청·오류를 서비스 간 추적하고 민감 정보 마스킹·보관 기간 확인 |
| ☐ | 장애 알림 | 가용성·오류율·지연·DB 상태의 기준, 수신 대상과 대응 절차를 정하고 테스트 알림 확인 |
| ☐ | `docs/devops/runbook.md` | 배포·앱 rollback·마이그레이션 실패·DB 복원·시크릿 교체 절차와 담당자 기록 |

## P3: 협업 편의와 확장

| 상태 | 항목 | 완료 기준·상세 근거 |
|:---:|---|---|
| ☑ | `.github/PULL_REQUEST_TEMPLATE.md` | [설정·검증 기록](records/collaboration.md#pr-템플릿) |
| ☑ | `.github/ISSUE_TEMPLATE/` | [설정·검증 기록](records/collaboration.md#이슈-템플릿) |
| ☐ | `CONTRIBUTING.md` | 개발·테스트·리뷰 절차 안내 |
| ☑ | 테스트 결과 PR 코멘트 게시 구성 | [설정·검증 기록](records/collaboration.md#테스트-결과-게시-구성) |
| ☑ | 테스트 결과 PR 코멘트 원격 동작 확인 | [설정·검증 기록](records/collaboration.md#테스트-결과-게시-원격-검증) |
| ☑ | 커밋 메시지 형식 검증 | [설정·검증 기록](records/collaboration.md#커밋-메시지-검증) |
| ☑ | 커밋 메시지 머지 커밋 허용 + 스크립트 테스트 CI 실행 | [설정·검증 기록](records/collaboration.md#커밋-메시지-머지-커밋-허용), [정책 결정](adr/0002-commit-message-merge-policy.md) |
| ☐ | 머지 커밋 허용의 원격 CI 확인 | Dependabot PR에서 머지 커밋 통과와 required check 통과 확인(main 반영 후) |
| ☐ | `Makefile` | 반복 명령 단축이 필요할 때 도입 |
| ☐ | `.devcontainer` | 개발 환경 공유가 필요할 때 도입 |
| ☐ | 서비스 카탈로그 | 서비스별 담당자·저장소·배포·대시보드·runbook 연결 |
| ☐ | 라이선스 검사(licensee 또는 ORT) | 허용·금지·예외 정책 적용 |
| ☐ | CHANGELOG·릴리스 자동화 | 버전·소스 커밋·이미지 digest 연결 |
| ☐ | monorepo 변화 감지 기반 선택 실행(서비스 확장 후) | 공통 모듈 변경 시 영향 서비스 검증 누락 방지 |

## 문서 관리

| 상태 | 항목 | 완료 기준·상세 근거 |
|:---:|---|---|
| ☑ | DevOps 문서 분리와 경로 정리 | [문서 구조 검증 기록](records/documentation.md) |

## 결정 필요

| 구분 | 내용 |
|---|---|
| 운영 배포 대상 | Kubernetes / ECS / VM(Compose 포함) — 공통 이미지·Compose는 구성했으며 실제 운영 런타임·인증·네트워크 구성 전에 결정 |
| 이미지 레지스트리 | GHCR / ACR / ECR |
| 이미지 빌드·배포 Runner | GitHub-hosted / self-hosted 결정 필요. 기존 품질 CI는 `ubuntu-latest` GitHub-hosted 사용 |
| 시크릿 관리 방식 | GitHub Environments secrets / 외부 시크릿 저장소 |
| 운영 승인·복구 목표 | 승인 담당자, RPO(허용 데이터 손실 시간), RTO(목표 복구 시간) |

결정은 `docs/devops/adr/`에 배경·선택·영향과 함께 기록한다. 통합 테스트 실행 방식은 기본 `test` 및 필수 CI 빌드에 포함하는 것으로 확정되어 결정 필요 목록에서 제외했다.
