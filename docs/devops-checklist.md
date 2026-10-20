# DevOps 체크리스트

완료 항목은 저장소에 반영된 순서대로 기록하고, 커밋 해시와 검증 근거를 함께 남긴다.
파일 작성만으로 완료 처리하지 않고, 각 항목의 성공 조건을 확인한다. GitHub 설정은 적용 대상과 확인 일자도 기록한다.

## 우선순위

| 구분 | 내용 |
|---|---|
| P0 | 다음 단계 진행 전에 확인할 품질·보안 기준과 운영 DB 검증 |
| P1 | 배포 가능한 이미지와 반복 가능한 개발·인증 환경 준비 |
| P2 | 첫 운영 배포 전 갖출 배포·복구·관찰성 구성 |
| P3 | 협업 편의와 서비스 확장에 따른 최적화 |

P0를 우선 진행하되, 독립적인 P1 로컬 컨테이너 구성은 병행할 수 있다. 배포 대상·레지스트리·시크릿 관리 방식은 이미지 게시·배포 인증 등 해당 결정에 의존하는 작업 전에 확정한다.
플랫폼 설정 활성화와 실제 차단 동작 검증은 별도 항목으로 관리한다. 아래 플랫폼 상태는 기록된 확인 시점 기준이다.

## 완료

| 상태 | 항목 | 완료 기준·설정·검증 근거 |
|:---:|---|---|
| ☑ | Spotless 포맷 검사 + `.editorconfig`·`.gitattributes` (BUILD 단계에 포함) | `35f2ab2` |
| ☑ | Dependabot(gradle·github-actions, weekly, minor·patch 그룹) | `601f601` |
| ☑ | JaCoCo 커버리지 보고서(html·xml)와 CI artifact 보관(7일) | `c661870` |
| ☑ | 실행 프로파일 분리(`prod` PostgreSQL + Flyway / `test` H2) | `e5f4d9b` (실제 PostgreSQL 마이그레이션 검증은 P0에 별도 기록) |
| ☑ | Gradle 실행 설정: 캐시·configuration cache·parallel·jvmargs (`gradle.properties`) | `bb507a8` |
| ☑ | 모듈별 라인 커버리지 게이트(기본 80%, `coverage.minimum`·`coverage.excludes`) | `183cb8c` (예외 정책과 실패 검증은 P0에 별도 기록) |
| ☑ | ArchUnit 계층 경계 검증(domain → 상위 계층·Spring 금지, application → 어댑터 금지, kernel → board-service 금지) | `f52022e` |
| ☑ | CI 트리거(main push + PR + 수동 실행)와 lint job 분리 | `0af2451` |

## P0: 선행 검증과 저장소 보호

| 상태 | 항목 | 완료 기준·설정·검증 근거 |
|:---:|---|---|
| ☑ | GitHub 보안 기능 사용 가능 여부 확인 | 저장소 public(2026-10-21 API 확인) → CodeQL·dependency review·secret scanning 무료 적용 대상 |
| ☑ | Secret scanning + push protection 활성화 | enabled 확인(2026-10-21), GitHub Settings에서 관리 |
| ☐ | Push protection 실제 차단 검증 | 2026-10-21 합성 패턴 4종(git push: GitHub PAT 형식 예시·AWS 예시 키쌍·랜덤 PAT 형식, contents API: PAT 형식)이 차단 없이 통과하고 alert 0건. GitHub은 자체 토큰을 검증하므로 합성 값으로는 차단이 발생하지 않는다. 확인 조건: 실 소유 토큰 1회 push(직후 revoke) 또는 org test mode — 개인 저장소에는 test mode 없음 |
| ☑ | CodeQL default setup 적용(파일 없음, 2026-10-21) | languages `java-kotlin`·`actions`, query suite default, threat model remote, runner standard |
| ☑ | CodeQL 첫 스캔 결과 확인(2026-10-21) | run `39482226154` job `Analyze (java-kotlin)`·`Analyze (actions)` success. main `3c9afa6` analysis: java-kotlin rules 76·results 0, actions rules 17·results 0. required check 이름 대신 ruleset의 `code_scanning`(tool CodeQL) 규칙으로 강제 |
| ☑ | Dependabot alerts 활성화(2026-10-21, 사용자 UI 작업) | alerts API가 403(`disabled`)에서 200(open alerts 0)으로 변경 확인 |
| ☑ | Dependabot automated security updates 활성화(2026-10-21, 사용자 UI) | UI 옵션 이름은 `Dependabot security updates`(automated security updates와 동일 기능). 활성화 후 API로 `enabled` 확인. alert 0건이라 실행 결과(security PR 생성)는 alert 발생으로 확인 필요 |
| ☐ | dependency graph에 Gradle·Java 의존성 포함 확인 | 2026-10-21 SBOM은 Actions 패키지 5개만 포함. Gradle deps가 그래프에 없으면 alerts·dependency review가 Java 의존성에 실효성 없다(Gradle은 lockfile 기반 transitive support 없음) |
| ☑ | rulesets 적용(2026-10-21) | `main protection`(id `25500333`, enforcement active, bypass 없음): required checks `Check formatting`·`Build and test`, code_scanning CodeQL(security high_or_higher), PR 필수, non-fast-forward, deletion 금지 |
| ☑ | rulesets 실제 병합 차단 확인(2026-10-21) | PR #3의 spotless 위반 commit으로 `Check formatting` failure → `gh pr merge` 거부("the base branch policy prohibits the merge"), `mergeable_state: blocked`. 검증 후 PR close + branch 삭제 |
| ☐ | CODEOWNERS 파일 + `require_code_owner_review` | 실제 리뷰 가능한 담당자를 지정하고 필요한 경로의 코드 소유자 승인 강제 확인. 현재 파일 없음·승인 조건 off |
| ☐ | dependency review 차단 정책 | PR dependency diff 경고는 플랫폼 UI(advisory)로 보이지만 병합 차단에는 workflow 파일 1개가 필요. 파일을 늘리지 않는다면 advisory + Dependabot alert로 대응한다는 결정을 기록 |
| ☑ | launcher 커버리지 예외 정책 확정 | 실행 진입점·모듈 조립에 한정해 게이트 제외 유지. [정책 ADR](adr/0001-launcher-coverage-policy.md) 참조(2026-10-21 확정) |
| ☑ | 커버리지 게이트 실패·제외 동작 검증 | 현재 launcher 제외 설정과 기준 변경 동작 확인. 아래 검증 기록 참조 |
| ☐ | launcher 역할 제한 ArchUnit 검증 | 운영 코드를 진입점·승인된 설정 클래스로 제한하고 허용하지 않은 클래스 추가 시 실패하는지 확인 |
| ☑ | Testcontainers PostgreSQL 기반 Flyway 통합 테스트 | prod 설정으로 빈 DB 마이그레이션·JPA `validate`, 저장·조회, 같은 DB로 앱 재기동 후 마이그레이션·데이터 유지 검증. 아래 실행 기록 참조 — `f9871f1` |
| ☑ | 통합 테스트 CI 실행 경로 구성 | 기본 `test`에 포함되어 기존 필수 `Build and test`의 `./gradlew build`에서 실행. Docker 사전 확인 추가, Docker 미사용 시 skip 없이 실패 |
| ☑ | PostgreSQL 통합 테스트 원격 CI 결과 확인 | run `39493409122` artifact `test-reports`: `PostgresFlywayIntegrationTest` 포함, testsuite 12개·tests 200 성공. Docker 사전 확인 step 실행 확인(2026-10-21) |

## P1: 이미지와 실행 환경 준비

| 상태 | 항목 | 완료 기준·설정·검증 근거 |
|:---:|---|---|
| ☐ | 배포 대상·레지스트리·Runner·시크릿 관리 방식 결정 | 아래 결정 항목을 ADR에 기록한 뒤 관련 구성 진행 |
| ☐ | `Dockerfile`(multi-stage 또는 layered jar, 비루트 사용자, OCI 레이블) | 빌드한 이미지가 비루트로 실행되고 외부 DB 설정으로 기동되는지 확인 |
| ☐ | `.dockerignore` | Git 메타데이터·시크릿·불필요한 로컬 산출물이 빌드 컨텍스트에 포함되지 않는지 확인 |
| ☐ | `compose.yaml`(PostgreSQL + healthcheck, 앱의 `depends_on: service_healthy`) | 새 볼륨에서 DB·앱 기동 및 재기동 확인 |
| ☐ | `.env.example` | 필요한 환경 변수·설명·안전한 예시를 제공하고 실제 자격 증명은 포함하지 않음 |
| ☐ | `scripts/` | 문서화된 명령으로 로컬 기동·종료·검증을 재현하고 데이터 삭제 명령은 구분 |
| ☐ | 이미지 빌드·push와 git SHA·릴리스 태그 정책 | 소스 커밋과 이미지 digest 추적, `0.0.1-SNAPSHOT` 버전 정책 정리 |
| ☐ | SBOM 생성(cyclonedx 또는 syft) | 해당 이미지 digest와 연결해 보관 |
| ☐ | 이미지 스캔(Trivy 등) | 차단 심각도와 예외 승인·만료 정책을 정하고 취약 이미지 차단 확인 |
| ☐ | GitHub Environments(dev/staging/prod)와 배포 인증 | 클라우드 사용 시 OIDC 신뢰 조건을 저장소·환경에 한정하고 최소 권한 확인 |
| ☐ | `SECURITY.md` | 취약점 신고 경로와 지원 범위 명시 |
| ☐ | `LICENSE` | 배포·공개 범위에 맞는 라이선스를 결정하고 파일 반영 |

## P2: 첫 운영 배포 전 필수 구성

| 상태 | 항목 | 완료 기준·설정·검증 근거 |
|:---:|---|---|
| ☐ | 런타임 정의(Kubernetes manifest/Helm, ECS task definition 또는 VM 실행 구성) | 리소스·포트·환경 변수·재시작 정책을 선언하고 재현 가능한 기동 확인 |
| ☐ | probe(liveness/readiness)와 종료 정책 | 트래픽 수신 조건과 프로세스 재시작 조건을 분리하고 종료 유예 시간 내 요청 처리 확인 |
| ☐ | HikariCP 풀·연결 타임아웃 | DB 연결 한도와 장애 시 응답 시간을 고려해 설정하고 연결 실패 동작 확인 |
| ☐ | 배포 워크플로(rollout → readiness/스모크 테스트 → 실패 시 앱 rollback) | 실패 배포를 주입해 이전 이미지 digest 복구 확인 |
| ☐ | 이미지 승격 정책 | staging에서 검증한 동일 digest를 prod에 배포하고 환경마다 다시 빌드하지 않음 |
| ☐ | 배포 동시 실행 제한과 운영 승인 | 환경별 직렬화와 진행 중 배포 취소 정책을 정하고 승인·실패 처리 확인 |
| ☐ | DB 마이그레이션 실행 주체·시점·실패 대응 | 이전 앱 버전과 호환되는 스키마 변경 원칙을 정하고 앱 rollback과 DB 복구 절차를 분리 |
| ☐ | DB 백업·복원 | 보관 기간·암호화·접근 권한과 목표 RPO·RTO를 정하고 별도 환경에서 실제 복원 검증 |
| ☐ | TLS·도메인·네트워크 접근 제어 | 인증서 갱신과 외부 노출 범위를 정하고 DB 외부 접근 제한 확인 |
| ☐ | DB 최소 권한 계정과 시크릿 수명 관리 | 앱·마이그레이션 권한을 구분하고 자격 증명 주입·교체 절차 확인 |
| ☐ | 관찰성 연결(actuator + Prometheus + OTel) | 카탈로그 선언과 실제 연결을 구분하고, 헬스·메트릭·trace 수집 및 관리 엔드포인트 접근 제한 확인 |
| ☐ | JSON 구조화 로그와 trace 전파 | 요청·오류를 서비스 간 추적하고 민감 정보 마스킹·보관 기간 확인 |
| ☐ | 장애 알림 | 가용성·오류율·지연·DB 상태의 기준, 수신 대상과 대응 절차를 정하고 테스트 알림 확인 |
| ☐ | `docs/runbook.md` | 배포·앱 rollback·마이그레이션 실패·DB 복원·시크릿 교체 절차와 담당자 기록 |

## P3: 협업 편의와 확장

| 상태 | 항목 | 완료 기준·설정·검증 근거 |
|:---:|---|---|
| ☐ | `.github/PULL_REQUEST_TEMPLATE.md` | 변경 목적·검증 결과·운영 영향 기록 |
| ☐ | `.github/ISSUE_TEMPLATE/` | 버그 재현 조건과 작업 완료 기준 기록 |
| ☐ | `CONTRIBUTING.md` | 개발·테스트·리뷰 절차 안내 |
| ☐ | 테스트 결과 PR 코멘트 게시 | fork PR에서도 권한·신뢰 경계를 유지하며 결과 제공 |
| ☐ | 커밋 메시지 형식 검증 | AGENTS.md의 `<아이콘> <type>: <한국어 요약>`과 squash merge 정책에 맞춰 검사 대상 결정 |
| ☐ | `Makefile` | 반복 명령 단축이 필요할 때 도입 |
| ☐ | `.devcontainer` | 개발 환경 공유가 필요할 때 도입 |
| ☐ | 서비스 카탈로그 | 서비스별 담당자·저장소·배포·대시보드·runbook 연결 |
| ☐ | 라이선스 검사(licensee 또는 ORT) | 허용·금지·예외 정책 적용 |
| ☐ | CHANGELOG·릴리스 자동화 | 버전·소스 커밋·이미지 digest 연결 |
| ☐ | monorepo 변화 감지 기반 선택 실행(서비스 확장 후) | 공통 모듈 변경 시 영향 서비스 검증 누락 방지 |

## 결정 필요

| 구분 | 내용 |
|---|---|
| 배포 대상 | Kubernetes / ECS / VM — 런타임·인증·네트워크 구성 전에 결정 |
| 이미지 레지스트리 | GHCR / ACR / ECR |
| Runner | GitHub-hosted / self-hosted |
| 시크릿 관리 방식 | GitHub Environments secrets / 외부 시크릿 저장소 |
| 통합 테스트 | 기본 `test`에 포함 / 태그로 분리하되 필수 CI job에서 실행 |
| 운영 승인·복구 목표 | 승인 담당자, RPO(허용 데이터 손실 시간), RTO(목표 복구 시간) |

결정은 `docs/adr/`에 배경·선택·영향과 함께 기록한다.

## 플랫폼 설정 상태 (2026-10-21 확인·적용)

파일로 관리되지 않는 GitHub Settings 영역 항목은 이 섹션에 기록한다. 저장소 public, account `lilamaris`. 문서 정리 시 원격 설정을 재조회하지 않았으며, 기존 기록 중 최신 확인 결과를 반영했다.

| 항목 | 상태 | 근거 |
|---|---|---|
| Secret scanning | enabled | `security_and_analysis.secret_scanning` |
| Push protection | enabled | `security_and_analysis.secret_scanning_push_protection`, open alerts 0 |
| Non-provider patterns / validity checks | disabled | 동일 API |
| CodeQL | configured + 첫 실행 완료(2026-10-21) | `code-scanning/default-setup` → `state: configured`; run `39482226154` both jobs success; main analysis results 0 |
| Dependabot alerts | enabled | 사용자 UI 활성화 후 alerts API 200, open alerts 0 |
| Dependabot automated security updates | enabled(2026-10-21 사용자 UI) | `security_and_analysis.dependabot_security_updates` → enabled |
| Dependency graph | Actions 패키지 5개만 포함 | `dependency-graph/sbom` → actions 4 + repository, Gradle·Java deps 없음 |
| Dependabot version updates | 동작 | `dynamic/dependabot/dependabot-updates` run success, PR #1·#2 생성 |
| Rulesets | active | id `25500333`, `current_user_can_bypass: never` |
| CI | green | run #5 push(main) success, run #3·#4는 PR #1·#2 각 1회 → 중복 실행 없음 |

설정 적용 당시 사용한 명령(이력용이며, 입력 JSON 파일은 저장소에 보관하지 않음):

```bash
# CodeQL default setup: state=configured, languages java-kotlin·actions, query suite default, threat model remote
gh api -X PATCH repos/lilamaris/lilboard/code-scanning/default-setup --input cq.json

# ruleset: required checks Check formatting·Build and test + code_scanning(CodeQL, high_or_higher) + PR 필수 + non-fast-forward + deletion 금지
gh api -X POST repos/lilamaris/lilboard/rulesets --input ruleset.json
```

CodeQL languages는 GET 응답에서 `[]`로 표시된 기록이 있으나, run `39482226154`의 `Analyze (java-kotlin)`·`Analyze (actions)` job은 모두 성공했다. 언어·실행 확인은 해당 job 결과를 근거로 한다.

## 검증 메모

- `check`와 `jacocoTestCoverageVerification`은 `java.base` 컨벤션에서 명시적으로 연결했다.
- JaCoCo violation rule의 `excludes`는 `element`가 지정한 대상의 이름에 적용된다. 기본 `element`는 `BUNDLE`이므로 현재 `*launcher*`는 번들 이름을 대상으로 한다. `CLASS` 규칙에서는 클래스 이름 패턴을 사용할 수 있다. 모듈 제외와 클래스 제외를 구분한다. [Gradle 공식 문서](https://docs.gradle.org/current/javadoc/org/gradle/testing/jacoco/tasks/rules/JacocoViolationRule.html)
- 기존 커버리지 메모(kernel·domain·jpa 100%, application 81.8%, launcher 33.3%)는 측정 커밋과 명령이 남아 있지 않아 참고값으로만 취급한다. 다음 측정부터 커밋 SHA·작업 트리 변경 여부·측정 일자·명령·보고서 위치를 함께 기록한다. 보고서 생성 명령은 `./gradlew jacocoTestReport`이며, 강제 재측정이 필요하면 `--rerun-tasks`를 사용한다.
- GitHub 보안 기능은 저장소 공개 범위·소유 형태·요금제에 따라 사용 가능 여부가 다르다. 비공개 저장소의 CodeQL·dependency review는 Code Security 등 사용 조건을 먼저 확인한다. secret scanning·push protection은 별도로 확인하며, 단순히 Security 탭을 활성화하는 것으로 완료 처리하지 않는다. [CodeQL 사용 조건](https://docs.github.com/en/code-security/reference/code-scanning/troubleshoot-analysis-errors/private-repository-enablement), [Dependency review 사용 조건](https://docs.github.com/en/code-security/concepts/supply-chain-security/dependency-review)
- CODEOWNERS 파일과 승인 강제 설정은 별개다. branch protection 또는 rulesets에서 코드 소유자 승인 조건을 적용하고 실제 병합 차단을 확인한다.
- ruleset 차단 검증(PR #3, 2026-10-21): required check failure이 차단 근거다. `Check formatting` 실패로 `Build and test`는 `needs: lint` 때문에 `skipped`로 기록됐고 CodeQL check은 success → `code_scanning` 규칙은 alert 없음(high_or_higher 기준)으로 충족. skipped required check만으로도 merge가 통과하는지는 별도로 확인해야 한다(GitHub는 skipped를 통과로 취급할 수 있음).
- push protection은 합성 값으로 검증되지 않는다(2026-10-21, 4회 시도·alert 0). GitHub PAT는 GitHub이 실 존재를 검증하고, 합성 값은 차단되지 않는다. 개인 저장소에는 org test mode가 없어 확인 조건은 실 토큰 push 후 revoke뿐이다.
- ruleset 게이트 실행 관찰(2026-10-21): required check 통과 후에도 CodeQL check이 `neutral`(실행 중)인 동안 `mergeable_state`는 blocked였고, CodeQL success 후 clean으로 전환됐다. 즉 code_scanning 규칙은 check 결과를 실제로 요구한다.
- PR 이력: `#1` gradle-minor-patch group → `1c2daa1`, `#2` junit-bom 5.13.4→6.1.3 → `99434fa`, `#4` 로컬 commit 원격 반영 → `56f667d`. `#2`는 PR #1과 `libs.versions.toml` 인접 라인 conflict로 `gh pr update-branch`가 거부됐고 Dependabot이 branch를 재배치한 뒤 병합 가능했다.
- 로컬 commit 3개(`f9871f1`·`a1be13c`·`6aa2078`)는 원격 main에 없었고 ruleset이 main direct push를 차단하므로 PR 경로로 반영했다(PR #4). main 반영은 PR 통해서만 가능하다는 운영 전제 확정.

## 커버리지 게이트 동작 검증 기록

| 구분 | 내용 |
|---|---|
| 검증 일자 | 2026-10-21 (Asia/Seoul) |
| 검증 대상 커밋 | `3c9afa62c95d95278f763feff42260658fe47fd7` |
| 실행 전 작업 트리 | 변경 없음. 실행 후 이 문서에 결과만 기록했다. |
| 기본 설정 | `coverage.minimum=0.80`, `coverage.excludes=*launcher*` |
| 보충 | 설정 파일은 수정하지 않고 `-P` 인자로 검증 조건을 변경했다. |
| 실행 명령 | 기대 결과 | 실제 결과 |
|---|---|---|
| `./gradlew build --rerun-tasks --console=plain` | 전체 테스트·기본 게이트 통과 | 성공, 52개 작업 실행. launcher의 낮은 커버리지는 현재 제외 설정에 따라 허용 |
| `./gradlew :board-service:launcher:check -Pcoverage.excludes= --console=plain` | launcher 포함 시 80% 미달로 실패 | 종료 코드 1, `jacocoTestCoverageVerification`에서 `0.33 < 0.80` 위반으로 실패 |
| `./gradlew :board-service:launcher:check -Pcoverage.excludes= -Pcoverage.minimum=0.30 --console=plain` | 동일 대상에서 기준을 30%로 낮추면 통과 | 성공 |

기준 미달 실패가 `check`까지 전파되고, 제외 설정과 기준값이 실제 결과에 반영됨을 확인했다.
launcher 제외 유지 정책은 [ADR](adr/0001-launcher-coverage-policy.md)로 확정했다. 역할 제한 ArchUnit 검증은 별도 P0 작업으로 남아 있다.

전체 테스트 재실행 후 모듈별 `build/reports/jacoco/test/jacocoTestReport.xml`의 최상위 `LINE` 카운터를 측정했다.

| 모듈 | 실행된 라인 / 전체 라인 | 라인 커버리지 |
|---|---|---|
| kernel | 200 / 200 | 100% |
| domain | 11 / 11 | 100% |
| application | 12 / 12 | 100% |
| jpa | 1 / 1 | 100% |
| web | 4 / 4 | 100% |
| launcher | 1 / 3 | 33.3% |

## PostgreSQL·Flyway 통합 테스트 실행 기록

| 구분 | 내용 |
|---|---|
| 검증 일자 | 2026-10-21 (Asia/Seoul), `f9871f1`에 반영된 테스트를 커밋 전 작업 트리에서 검증 |
| 테스트 | `board-service/launcher/src/test/java/com/lilamaris/lilboard/board/launcher/PostgresFlywayIntegrationTest.java` |
| DB | Testcontainers `postgres:17.6-alpine`. 향후 Compose·운영 버전 결정 시 함께 정렬한다. |
| 설정 | 기존 `prod` 프로파일(Flyway 활성화, JPA `validate`)을 사용하며, 컨테이너의 임시 접속 정보를 명령행 인자로 전달한다. H2 `test` 프로파일은 사용하지 않는다. |
| 시나리오 | 빈 DB 최초 기동·V1 적용, 게시판 저장·조회, 같은 DB 컨테이너를 유지하고 앱 컨텍스트 종료·재생성 후 V1 중복 적용 없이 데이터 유지 |
| 명령 | `./gradlew :board-service:launcher:test --tests '*PostgresFlywayIntegrationTest' --console=plain` |
| 결과 | 테스트 3개 성공, 실패·오류·skip 0. 후속 `./gradlew build --console=plain`도 성공하여 기존 테스트·포맷·커버리지 게이트 통과 확인. 보고서: `board-service/launcher/build/reports/tests/test/index.html` |
| CI | 별도 태그·job으로 분리하지 않고 기본 테스트에 포함한다. 컨테이너는 테스트마다 새로 생성·종료하고, 재기동 시나리오에서만 두 앱 컨텍스트가 같은 DB를 사용한다. |
| 보충 | 원격 CI 실행 결과는 아직 확인하지 않았다. 별도 P0 확인 항목으로 추적한다. |
