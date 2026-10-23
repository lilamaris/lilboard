# 저장소 보호와 병합 차단

[DevOps 체크리스트](../checklist.md)의 상세 설정과 검증 근거를 기록한다. 날짜·커밋·실행 결과는 기존 확인 시점의 기록이며, 문서 분리 과정에서 원격 설정이나 런타임을 재검증하지 않았다.

## Ruleset 설정

대상: rulesets 적용(2026-10-21)

`main protection`(id `25500333`, enforcement active, bypass 없음): required checks `Check formatting`·`Build and test`, code_scanning CodeQL(security high_or_higher), PR 필수, non-fast-forward, deletion 금지

## 병합 차단 검증

대상: rulesets 실제 병합 차단 확인(2026-10-21)

PR #3의 spotless 위반 commit으로 `Check formatting` failure → `gh pr merge` 거부("the base branch policy prohibits the merge"), `mergeable_state: blocked`. 검증 후 PR close + branch 삭제

## 코드 소유자 승인

대상: CODEOWNERS 파일 + `require_code_owner_review`

실제 리뷰 가능한 담당자를 지정하고 필요한 경로의 코드 소유자 승인 강제 확인. 현재 파일 없음·승인 조건 off

## 플랫폼 설정 확인 기록

2026-10-21 확인·적용. 저장소 public, account `lilamaris`. GitHub Settings 영역의 기존 기록이며 문서 분리 시 원격 설정을 재조회하지 않았다.

| 항목 | 상태 | 근거 |
|---|---|---|
| Rulesets | active | id `25500333`, `current_user_can_bypass: never` |

설정 적용 당시 명령이다. 입력 JSON 파일은 저장소에 보관하지 않는다.

```bash
# ruleset: required checks Check formatting·Build and test + code_scanning(CodeQL, high_or_higher) + PR 필수 + non-fast-forward + deletion 금지
gh api -X POST repos/lilamaris/lilboard/rulesets --input ruleset.json
```

## 검증 메모

- CODEOWNERS 파일과 승인 강제 설정은 별개다. branch protection 또는 rulesets에서 코드 소유자 승인 조건을 적용하고 실제 병합 차단을 확인한다.
- ruleset 차단 검증(PR #3, 2026-10-21): required check failure이 차단 근거다. `Check formatting` 실패로 `Build and test`는 `needs: lint` 때문에 `skipped`로 기록됐고 CodeQL check은 success → `code_scanning` 규칙은 alert 없음(high_or_higher 기준)으로 충족. skipped required check만으로도 merge가 통과하는지는 별도로 확인해야 한다(GitHub는 skipped를 통과로 취급할 수 있음).
- ruleset 게이트 실행 관찰(2026-10-21): required check 통과 후에도 CodeQL check이 `neutral`(실행 중)인 동안 `mergeable_state`는 blocked였고, CodeQL success 후 clean으로 전환됐다. 즉 code_scanning 규칙은 check 결과를 실제로 요구한다.
- 로컬 commit 3개(`f9871f1`·`a1be13c`·`6aa2078`)는 원격 main에 없었고 ruleset이 main direct push를 차단하므로 PR 경로로 반영했다(PR #4). main 반영은 PR 통해서만 가능하다는 운영 전제 확정.
