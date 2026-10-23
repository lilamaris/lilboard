# 의존성 관리

[DevOps 체크리스트](../checklist.md)의 상세 설정과 검증 근거를 기록한다. 날짜·커밋·실행 결과는 기존 확인 시점의 기록이며, 문서 분리 과정에서 원격 설정이나 런타임을 재검증하지 않았다.

## 버전 업데이트

대상: Dependabot(gradle·github-actions, weekly, minor·patch 그룹)

`601f601`

## 보안 알림

대상: Dependabot alerts 활성화(2026-10-21, 사용자 UI 작업)

alerts API가 403(`disabled`)에서 200(open alerts 0)으로 변경 확인

## 보안 업데이트

대상: Dependabot automated security updates 활성화(2026-10-21, 사용자 UI)

UI 옵션 이름은 `Dependabot security updates`(automated security updates와 동일 기능). 활성화 후 API로 `enabled` 확인. alert 0건이라 실행 결과(security PR 생성)는 alert 발생으로 확인 필요

## 의존성 그래프

대상: dependency graph에 Gradle·Java 의존성 포함 확인

2026-10-21 SBOM은 Actions 패키지 5개만 포함. Gradle deps가 그래프에 없으면 alerts·dependency review가 Java 의존성에 실효성 없다(Gradle은 lockfile 기반 transitive support 없음)

## Dependency review

대상: dependency review 차단 정책

PR dependency diff 경고는 플랫폼 UI(advisory)로 보이지만 병합 차단에는 workflow 파일 1개가 필요. 파일을 늘리지 않는다면 advisory + Dependabot alert로 대응한다는 결정을 기록

## 플랫폼 설정 확인 기록

2026-10-21 확인·적용. 저장소 public, account `lilamaris`. GitHub Settings 영역의 기존 기록이며 문서 분리 시 원격 설정을 재조회하지 않았다.

| 항목 | 상태 | 근거 |
|---|---|---|
| Dependabot alerts | enabled | 사용자 UI 활성화 후 alerts API 200, open alerts 0 |
| Dependabot automated security updates | enabled(2026-10-21 사용자 UI) | `security_and_analysis.dependabot_security_updates` → enabled |
| Dependency graph | Actions 패키지 5개만 포함 | `dependency-graph/sbom` → actions 4 + repository, Gradle·Java deps 없음 |
| Dependabot version updates | 동작 | `dynamic/dependabot/dependabot-updates` run success, PR #1·#2 생성 |

## 검증 메모

- PR 이력: `#1` gradle-minor-patch group → `1c2daa1`, `#2` junit-bom 5.13.4→6.1.3 → `99434fa`, `#4` 로컬 commit 원격 반영 → `56f667d`. `#2`는 PR #1과 `libs.versions.toml` 인접 라인 conflict로 `gh pr update-branch`가 거부됐고 Dependabot이 branch를 재배치한 뒤 병합 가능했다.
