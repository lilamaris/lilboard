# 협업 템플릿과 테스트 결과 게시

[DevOps 체크리스트](../checklist.md)의 상세 설정과 검증 근거를 기록한다. 날짜·커밋·실행 결과는 기존 확인 시점의 기록이며, 문서 분리 과정에서 원격 설정이나 런타임을 재검증하지 않았다.

## PR 템플릿

대상: `.github/PULL_REQUEST_TEMPLATE.md`

변경 내용(목적·운영 영향)·검증·관련 이슈의 간결한 Markdown 템플릿 추가. GitHub 화면 적용은 기본 브랜치 반영 후 확인

## 이슈 템플릿

대상: `.github/ISSUE_TEMPLATE/`

버그 보고(문제·재현 방법)와 기능·개선 요청(목적·완료 기준) Markdown 템플릿 추가. GitHub 화면 적용은 기본 브랜치 반영 후 확인

## 테스트 결과 게시 구성

대상: 테스트 결과 PR 코멘트 게시 구성

`test-results.yml`에서 CI 완료 후 JUnit XML을 집계해 요약 코멘트 갱신. 테스트 실행은 읽기 권한 CI에 유지하고, fork·Dependabot PR의 결과 게시만 별도 권한으로 실행. actionlint 및 게시 전 조건 분기 8개 검증 통과

## 테스트 결과 게시 원격 검증

대상: 테스트 결과 PR 코멘트 원격 동작 확인

기본 브랜치 반영 후 일반·fork PR에서 성공·실패 요약과 기존 코멘트 갱신 확인(UI 확인 완료).

## 커밋 메시지 검증

대상: 커밋 메시지 형식 검증

`.github/scripts/validate-commit-messages.py`와 CI job(`.github/workflows/ci.yml`의 `commit-message`) 추가. AGENTS.md `<아이콘> <type>: <한국어 요약>` 형식(6개 타입)을 PR의 각 커밋 메시지로 검증. 스쿼시 머지 시 스쿼시 커밋 메시지도 동일 형식을 따른다. 검증 근거: `efec537`. GitHub ruleset(`main protection`, id `25500333`)의 required check에 `Validate commit messages`를 추가해 CI 실패 시 머지 차단(UI 적용 확인 완료).

## 커밋 메시지 머지 커밋 허용

대상: 머지 커밋를 허용하는 커밋 메시지 검증

정책 결정: [ADR 0002](../adr/0002-commit-message-merge-policy.md). 머지 커밋는 parent가 2개 이상으로 판정하고, 기계가 생성한 머지 메시지(`Merge branch`·`Merge remote-tracking branch`·`Merge pull request`·`Merge tag`)와 AGENTS.md 형식을 허용한다. 위장 머지 메시지와 임의 메시지의 2-parent 커밋는 실패 처리하고, 일반 커밋에는 기존 형식만 적용한다.

원인 재현: Dependabot 브랜치가 main을 머지해 만든 `Merge branch 'main' into ...` 커밋(base `8921e44`, head `07051922`)가 개선 전 스크립트에서 실패(exit 1)했고, 개선 후 통과한다.

로컬 검증(2026-10-28): `python3 -m unittest discover -s .github/scripts/tests` 22개 통과. 임시 git 저장소 회귀 테스트로 main을 머지한 PR 브랜치 통과, `Merge branch: main 반영` 위장 머지 실패, 비준수 커밋 실패, 커밋 객체가 없는 입력의 환경 실패(exit 2)를 확인했다. 실제 저장소 main 전 역사(비머지 65개·머지 13개)에서도 스크립트가 통과한다.

CI 구성: `commit-message` job의 검증 전에 `python3 -m unittest discover -s .github/scripts/tests` 단계를 추가했다. actionlint v1.7.12 로컬 통과.

원격 CI 확인(2026-10-28, PR #22, [run `40622043210`](https://github.com/lilamaris/lilboard/actions/runs/40622043210)): `Validate commit messages` job의 `Run commit message script tests` 단계가 `Ran 22 tests`·`OK`로 통과했고, `Validate commit message format`이 `✅ 1개 커밋 메시지가 형식을 따릅니다`로 통과했다. 같은 run의 `Check formatting`과 `Build and test`도 통과했다. 이 PR은 머지 커밋를 포함하지 않아 머지 커밋 허용 자체의 원격 확인은 다음 Dependabot PR에서 수행한다.

미확인 범위: Dependabot PR의 머지 커밋 통과와 required check 통과(원격 CI)는 main 반영 후 실제 PR 실행으로 확인해야 한다(체크리스트 ☐). main push의 `Merge pull request #N` 커밋는 job이 `pull_request` 전용이므로 검증 대상이 아니다.
