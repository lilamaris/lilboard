# DevOps 체크리스트

완료 항목은 저장소에 반영된 순서대로 기록하고, 커밋 해시와 검증 근거를 함께 남긴다.
파일 작성만으로 완료 처리하지 않고, 각 항목의 성공 조건을 확인한다. GitHub 설정은 적용 대상과 확인 일자도 기록한다.

## 우선순위

- P0: 다음 단계 진행 전에 확인할 품질·보안 기준과 운영 DB 검증
- P1: 배포 가능한 이미지와 반복 가능한 개발·인증 환경 준비
- P2: 첫 운영 배포 전 갖출 배포·복구·관찰성 구성
- P3: 협업 편의와 서비스 확장에 따른 최적화

P0 → P1 → P2 순서로 진행하되, 배포 대상·레지스트리·시크릿 관리 방식은 관련 P1 작업 전에 결정한다.

## 완료

- [x] Spotless 포맷 검사 + `.editorconfig`·`.gitattributes` (BUILD 단계에 포함) — `35f2ab2`
- [x] Dependabot(gradle·github-actions, weekly, minor·patch 그룹) — `601f601`
- [x] JaCoCo 커버리지 보고서(html·xml)와 CI artifact 보관(7일) — `c661870`
- [x] 실행 프로파일 분리(`prod` PostgreSQL + Flyway / `test` H2) — `e5f4d9b` (실제 PostgreSQL 마이그레이션 검증은 P0에 별도 기록)
- [x] Gradle 실행 설정: 캐시·configuration cache·parallel·jvmargs (`gradle.properties`) — `bb507a8`
- [x] 모듈별 라인 커버리지 게이트(기본 80%, `coverage.minimum`·`coverage.excludes`) — `183cb8c` (예외 정책과 실패 검증은 P0에 별도 기록)
- [x] ArchUnit 계층 경계 검증(domain → 상위 계층·Spring 금지, application → 어댑터 금지, kernel → board-service 금지) — `f52022e`
- [x] CI 트리거(main push + PR + 수동 실행)와 lint job 분리 — `0af2451`

## P0: 선행 검증과 저장소 보호

- [ ] GitHub 보안 기능의 저장소 공개 범위·소유 형태·요금제별 사용 가능 여부 확인 — CodeQL·dependency review·secret scanning·push protection 각각의 사용 여부와 대안을 기록
- [ ] CodeQL 분석 구성 — 사용 가능 조건 확인 후 default setup 또는 workflow를 선택하고, 실제 분석 결과 확인
- [ ] `actions/dependency-review-action` (PR 의존성 변화 검사) — 사용 가능 조건과 차단할 취약점 심각도·라이선스 정책을 정하고 PR 검사 확인
- [ ] 시크릿 스캔(gitleaks 또는 GitHub secret scanning + push protection) — 검사 범위와 예외 정책을 정하고 테스트용 탐지 사례로 검증
- [ ] GitHub branch protection 또는 rulesets와 required checks 적용 — 필요한 CI 실패 시 병합이 차단되는지, 관리자 우회 정책까지 확인
- [ ] launcher 커버리지 예외 정책 확정 — 모듈 제외 또는 별도 하한 등을 결정하고, 적용 대상과 이유를 문서화
- [ ] 커버리지 게이트 실패·제외 동작 검증 — 포함 모듈은 기준 미달 시 실패하고 launcher는 확정한 정책대로 처리되는지 확인
- [ ] Testcontainers PostgreSQL 기반 Flyway 통합 테스트 — 빈 DB에 마이그레이션 적용 후 JPA `validate`와 저장·조회 성공, 재기동 시 기존 마이그레이션 재적용 없이 성공하는지 확인
- [ ] 통합 테스트 CI 실행 경로 보장 — 분리할 경우 `@Tag("integration")` 필터와 별도 job을 함께 구성하고 필수 검사에 포함해 테스트 누락 방지

## P1: 이미지와 실행 환경 준비

- [ ] 배포 대상·레지스트리·Runner·시크릿 관리 방식 결정 — 아래 결정 항목을 ADR에 기록한 뒤 관련 구성 진행
- [ ] `Dockerfile`(multi-stage 또는 layered jar, 비루트 사용자, OCI 레이블) — 빌드한 이미지가 비루트로 실행되고 외부 DB 설정으로 기동되는지 확인
- [ ] `.dockerignore` — Git 메타데이터·시크릿·불필요한 로컬 산출물이 빌드 컨텍스트에 포함되지 않는지 확인
- [ ] `compose.yaml`(PostgreSQL + healthcheck, 앱의 `depends_on: service_healthy`) — 새 볼륨에서 DB·앱 기동 및 재기동 확인
- [ ] `.env.example` — 필요한 환경 변수·설명·안전한 예시를 제공하고 실제 자격 증명은 포함하지 않음
- [ ] `scripts/` — 문서화된 명령으로 로컬 기동·종료·검증을 재현하고 데이터 삭제 명령은 구분
- [ ] 이미지 빌드·push와 git SHA·릴리스 태그 정책 — 소스 커밋과 이미지 digest 추적, `0.0.1-SNAPSHOT` 버전 정책 정리
- [ ] SBOM 생성(cyclonedx 또는 syft) — 해당 이미지 digest와 연결해 보관
- [ ] 이미지 스캔(Trivy 등) — 차단 심각도와 예외 승인·만료 정책을 정하고 취약 이미지 차단 확인
- [ ] GitHub Environments(dev/staging/prod)와 배포 인증 — 클라우드 사용 시 OIDC 신뢰 조건을 저장소·환경에 한정하고 최소 권한 확인
- [ ] `.github/CODEOWNERS` — 실제 리뷰 가능한 담당자를 지정하고 필요한 경로에 코드 소유자 승인 강제 확인
- [ ] `SECURITY.md` — 취약점 신고 경로와 지원 범위 명시
- [ ] `LICENSE` — 배포·공개 범위에 맞는 라이선스를 결정하고 파일 반영

## P2: 첫 운영 배포 전 필수 구성

- [ ] 런타임 정의(Kubernetes manifest/Helm, ECS task definition 또는 VM 실행 구성) — 리소스·포트·환경 변수·재시작 정책을 선언하고 재현 가능한 기동 확인
- [ ] probe(liveness/readiness)와 종료 정책 — 트래픽 수신 조건과 프로세스 재시작 조건을 분리하고 종료 유예 시간 내 요청 처리 확인
- [ ] HikariCP 풀·연결 타임아웃 — DB 연결 한도와 장애 시 응답 시간을 고려해 설정하고 연결 실패 동작 확인
- [ ] 배포 워크플로(rollout → readiness/스모크 테스트 → 실패 시 앱 rollback) — 실패 배포를 주입해 이전 이미지 digest 복구 확인
- [ ] 이미지 승격 정책 — staging에서 검증한 동일 digest를 prod에 배포하고 환경마다 다시 빌드하지 않음
- [ ] 배포 동시 실행 제한과 운영 승인 — 환경별 직렬화와 진행 중 배포 취소 정책을 정하고 승인·실패 처리 확인
- [ ] DB 마이그레이션 실행 주체·시점·실패 대응 — 이전 앱 버전과 호환되는 스키마 변경 원칙을 정하고 앱 rollback과 DB 복구 절차를 분리
- [ ] DB 백업·복원 — 보관 기간·암호화·접근 권한과 목표 RPO·RTO를 정하고 별도 환경에서 실제 복원 검증
- [ ] TLS·도메인·네트워크 접근 제어 — 인증서 갱신과 외부 노출 범위를 정하고 DB 외부 접근 제한 확인
- [ ] DB 최소 권한 계정과 시크릿 수명 관리 — 앱·마이그레이션 권한을 구분하고 자격 증명 주입·교체 절차 확인
- [ ] 관찰성 연결(actuator + Prometheus + OTel — 카탈로그 선언과 실제 연결 구분) — 헬스·메트릭·trace 수집 및 관리 엔드포인트 접근 제한 확인
- [ ] JSON 구조화 로그와 trace 전파 — 요청·오류를 서비스 간 추적하고 민감 정보 마스킹·보관 기간 확인
- [ ] 장애 알림 — 가용성·오류율·지연·DB 상태의 기준, 수신 대상과 대응 절차를 정하고 테스트 알림 확인
- [ ] `docs/runbook.md` — 배포·앱 rollback·마이그레이션 실패·DB 복원·시크릿 교체 절차와 담당자 기록

## P3: 협업 편의와 확장

- [ ] `.github/PULL_REQUEST_TEMPLATE.md` — 변경 목적·검증 결과·운영 영향 기록
- [ ] `.github/ISSUE_TEMPLATE/` — 버그 재현 조건과 작업 완료 기준 기록
- [ ] `CONTRIBUTING.md` — 개발·테스트·리뷰 절차 안내
- [ ] 테스트 결과 PR 코멘트 게시 — fork PR에서도 권한·신뢰 경계를 유지하며 결과 제공
- [ ] 커밋 메시지 형식 검증 — AGENTS.md의 `<아이콘> <type>: <한국어 요약>`과 squash merge 정책에 맞춰 검사 대상 결정
- [ ] `Makefile` — 반복 명령 단축이 필요할 때 도입
- [ ] `.devcontainer` — 개발 환경 공유가 필요할 때 도입
- [ ] 서비스 카탈로그 — 서비스별 담당자·저장소·배포·대시보드·runbook 연결
- [ ] 라이선스 검사(licensee 또는 ORT) — 허용·금지·예외 정책 적용
- [ ] CHANGELOG·릴리스 자동화 — 버전·소스 커밋·이미지 digest 연결
- [ ] monorepo 변화 감지 기반 선택 실행(서비스 확장 후) — 공통 모듈 변경 시 영향 서비스 검증 누락 방지

## 결정 필요

- 배포 대상: Kubernetes / ECS / VM — 런타임·인증·네트워크 구성 전에 결정
- 이미지 레지스트리: GHCR / ACR / ECR
- Runner: GitHub-hosted / self-hosted
- 시크릿 관리 방식: GitHub Environments secrets / 외부 시크릿 저장소
- launcher 모듈 게이트: 제외 유지 / 별도 하한과 점진적 상향 / `main()` 테스트 필요성 검토
- 통합 테스트: 기본 `test`에 포함 / 태그로 분리하되 필수 CI job에서 실행
- 운영 승인·복구 목표: 승인 담당자, RPO(허용 데이터 손실 시간), RTO(목표 복구 시간)

결정은 `docs/adr/`에 배경·선택·영향과 함께 기록한다.

## 검증 메모

- `check`와 `jacocoTestCoverageVerification`은 `java.base` 컨벤션에서 명시적으로 연결했다.
- JaCoCo violation rule의 `excludes`는 `element`가 지정한 대상의 이름에 적용된다. 기본 `element`는 `BUNDLE`이므로 현재 `*launcher*`는 번들 이름을 대상으로 한다. `CLASS` 규칙에서는 클래스 이름 패턴을 사용할 수 있다. 모듈 제외와 클래스 제외를 구분한다. [Gradle 공식 문서](https://docs.gradle.org/current/javadoc/org/gradle/testing/jacoco/tasks/rules/JacocoViolationRule.html)
- 기존 커버리지 메모(kernel·domain·jpa 100%, application 81.8%, launcher 33.3%)는 측정 커밋과 명령이 남아 있지 않아 참고값으로만 취급한다. 다음 측정부터 커밋 SHA·작업 트리 변경 여부·측정 일자·명령·보고서 위치를 함께 기록한다. 보고서 생성 명령은 `./gradlew jacocoTestReport`이며, 강제 재측정이 필요하면 `--rerun-tasks`를 사용한다.
- GitHub 보안 기능은 저장소 공개 범위·소유 형태·요금제에 따라 사용 가능 여부가 다르다. 비공개 저장소의 CodeQL·dependency review는 Code Security 등 사용 조건을 먼저 확인한다. secret scanning·push protection은 별도로 확인하며, 단순히 Security 탭을 활성화하는 것으로 완료 처리하지 않는다. [CodeQL 사용 조건](https://docs.github.com/en/code-security/reference/code-scanning/troubleshoot-analysis-errors/private-repository-enablement), [Dependency review 사용 조건](https://docs.github.com/en/code-security/concepts/supply-chain-security/dependency-review)
- CODEOWNERS 파일과 승인 강제 설정은 별개다. branch protection 또는 rulesets에서 코드 소유자 승인 조건을 적용하고 실제 병합 차단을 확인한다.
