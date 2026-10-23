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
