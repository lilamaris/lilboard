# 커밋 메시지 검증의 머지 커밋 허용 정책

- 상태: 확정
- 일자: 2026-10-28

## 배경

`commit-message` job는 PR의 `base.sha..head.sha` 범위 모든 커밋를 순회해 AGENTS.md의 `<아이콘> <type>: <한국어 요약>` 형식을 검사한다. `git log`는 모든 parent를 따라가므로 PR 브랜치가 main을 머지할 때 생성된 머지 커밋도 검사 대상이 된다.

Dependabot PR과 GitHub "Update branch"는 `Merge branch 'main' into ...` 머지 커밋를 만든다. 이 메시지는 커밋 형식을 따르지 않아 CI가 실패했다. 재현: base `8921e44`, head `07051922`(Dependabot 브랜치가 main을 머지).

머지 커밋의 요약은 사람이 작성한 변경 설명이 아니라 git과 GitHub가 생성한 기계 메시지다. 머지로 유입된 커밋는 각자의 PR에서 이미 같은 검증을 통과한다.

## 결정

- 머지 커밋는 메시지 형식이 아니라 parent 개수(2개 이상)로 판정한다. 메시지 정규식만으로는 git 버전·형식 변형(`of <url>` 포함, tag 머지)과 위장 메시지를 구분할 수 없다.
- 머지 커밋는 기계가 생성한 머지 메시지(`Merge branch`, `Merge remote-tracking branch`, `Merge pull request`, `Merge tag`)를 허용한다.
- 사람이 AGENTS.md 형식으로 작성한 머지 메시지도 허용한다. 저장소에 `🔧 chore: 커밋 메시지 검증이 추가된 main 반영` 머지 커밋가 이미 존재한다.
- 머지 커밋에 위 두 형식 모두 아닌 메시지는 실패 처리한다. 머지처럼 위장한 메시지(`Merge branch: main 반영`)와 임의 메시지의 2-parent 커밋를 차단한다.
- 일반 커밋(parent 1개)은 기존 커밋 형식만 적용한다. 머지 형식 메시지로 통과하지 못한다.
- 검증 범위는 `base.sha..head.sha` 전체 순회를 유지한다. main에서 유입된 커밋를 재검증하며, main의 비머지 커밋은 현재 전부 형식을 따르므로 실행에 영향이 없다.

## 선택하지 않은 안

- 머지 커밋 완전 면제(`--no-merges`): 구현이 가장 단순하지만, 임의 메시지의 2-parent 커밋를 검사하지 못해 머지 형식 위장을 차단하지 못한다.
- 머지 커밋 body의 PR 제목까지 검증: AGENTS.md의 PR 제목·스쿼시 메시지 규칙까지 CI로 강제할 수 있으나 스크립트 복잡도가 증가한다. 실제 필요 확인 후 후속 결정한다.
- `--first-parent` 순회: main 유입 커밋의 재검증을 없앨 수 있으나, 머지 커밋 정책과 범위를 함께 바꾸는 별도 결정이다.

## 영향

- 스크립트 테스트(unittest 22개)를 `commit-message` job의 검증 전에 실행해 머지 커밋 통과와 위장 머지 실패를 회귀로 고정한다.
- main push의 `Merge pull request #N` 커밋는 검증 대상이 아니다. job이 `pull_request` 전용이므로, main의 머지 메시지까지 검사하려면 push 트리거와 head 커밋 검증을 추가해야 한다.
- Dependabot PR에서 머지 커밋가 통과하는 최종 확인은 main 반영 후 실제 PR 실행으로 수행한다.
