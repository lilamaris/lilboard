# DevOps 문서

DevOps 관련 진행 상태, 상세 설정과 검증 기록, 실행 안내, 정책 결정을 이 디렉터리에서 관리한다.

| 위치 | 내용 |
|---|---|
| [체크리스트](checklist.md) | 우선순위, 항목별 상태·완료 기준, 상세 근거 링크, 결정 필요 사항 |
| [항목별 기록](#항목별-기록) | 설정 값, 완료 근거, 확인 일자, 커밋·실행 결과와 검증 한계 |
| [배포와 실행](deployment/README.md) | 이미지 구성·빌드, Compose·Docker CLI·JVM 실행 절차 |
| [ADR](adr/) | DevOps 정책 결정의 배경·선택·영향 |

## 항목별 기록

| 문서 | 대상 |
|---|---|
| [빌드와 품질 CI](records/ci-and-quality.md) | 포맷, Gradle 실행 설정, ArchUnit, CI 실행 경로 |
| [의존성 관리](records/dependencies.md) | Dependabot, 의존성 그래프, dependency review |
| [GitHub 보안](records/github-security.md) | 보안 기능 사용 범위, secret scanning·push protection, CodeQL |
| [저장소 보호](records/repository-protection.md) | Ruleset, 병합 차단, 코드 소유자 승인 |
| [커버리지](records/coverage.md) | 보고서, 모듈별 게이트, launcher 예외, 실패·제외 검증 |
| [PostgreSQL과 Flyway](records/postgres-flyway.md) | 프로파일, Flyway 적용, 재기동, 로컬·원격 CI 검증 |
| [컨테이너 이미지](records/container-image.md) | Dockerfile·빌드 컨텍스트, 이미지 빌드·기동 검증 |
| [Compose](records/compose.md) | 환경 변수 예시, API·재기동·데이터 유지 검증 |
| [협업](records/collaboration.md) | 이슈·PR 템플릿, 테스트 요약 게시, 커밋 메시지 검증 |
| [문서 구조](records/documentation.md) | 문서 분리와 경로·포맷 검증 |

## 갱신 방법

- DevOps 변경 시 체크리스트의 상태·완료 기준·상세 근거 링크를 함께 갱신한다. 완료 항목은 기존 반영 순서를 유지한다.
- 설정 값과 실행 이력은 해당 항목의 `records/` 문서에 기록한다. 확인 일자·대상 커밋 또는 실행 링크·환경·명령·결과·미확인 범위를 남긴다.
- 구성 완료와 실제 동작 확인, 로컬 검증과 원격 CI·운영 검증을 구분한다. 과거 결과는 당시 조건과 함께 보존한다.
- 실행 절차는 `deployment/`, 정책 결정의 배경·선택·영향은 `adr/`에 기록하고 관련 항목에서 연결한다.
- 새 항목의 상세 기록이 필요하면 항목별 문서를 추가하고 이 안내와 체크리스트에서 연결한다. 미구성 항목은 체크리스트의 완료 기준으로 추적한다.
