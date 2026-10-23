# GitHub 보안 설정과 검증

[DevOps 체크리스트](../checklist.md)의 상세 설정과 검증 근거를 기록한다. 날짜·커밋·실행 결과는 기존 확인 시점의 기록이며, 문서 분리 과정에서 원격 설정이나 런타임을 재검증하지 않았다.

## 사용 가능 범위

대상: GitHub 보안 기능 사용 가능 여부 확인

저장소 public(2026-10-21 API 확인) → CodeQL·dependency review·secret scanning 무료 적용 대상

## Secret scanning과 push protection 활성화

대상: Secret scanning + push protection 활성화

enabled 확인(2026-10-21), GitHub Settings에서 관리

## Push protection 차단 검증

대상: Push protection 실제 차단 검증

2026-10-21 합성 패턴 4종(git push: GitHub PAT 형식 예시·AWS 예시 키쌍·랜덤 PAT 형식, contents API: PAT 형식)이 차단 없이 통과하고 alert 0건. GitHub은 자체 토큰을 검증하므로 합성 값으로는 차단이 발생하지 않는다. 확인 조건: 실 소유 토큰 1회 push(직후 revoke) 또는 org test mode — 개인 저장소에는 test mode 없음

## CodeQL 설정

대상: CodeQL default setup 적용(파일 없음, 2026-10-21)

languages `java-kotlin`·`actions`, query suite default, threat model remote, runner standard

## CodeQL 실행 결과

대상: CodeQL 첫 스캔 결과 확인(2026-10-21)

run `39482226154` job `Analyze (java-kotlin)`·`Analyze (actions)` success. main `3c9afa6` analysis: java-kotlin rules 76·results 0, actions rules 17·results 0. required check 이름 대신 ruleset의 `code_scanning`(tool CodeQL) 규칙으로 강제

## 플랫폼 설정 확인 기록

2026-10-21 확인·적용. 저장소 public, account `lilamaris`. GitHub Settings 영역의 기존 기록이며 문서 분리 시 원격 설정을 재조회하지 않았다.

| 항목 | 상태 | 근거 |
|---|---|---|
| Secret scanning | enabled | `security_and_analysis.secret_scanning` |
| Push protection | enabled | `security_and_analysis.secret_scanning_push_protection`, open alerts 0 |
| Non-provider patterns / validity checks | disabled | 동일 API |
| CodeQL | configured + 첫 실행 완료(2026-10-21) | `code-scanning/default-setup` → `state: configured`; run `39482226154` both jobs success; main analysis results 0 |

설정 적용 당시 명령이다. 입력 JSON 파일은 저장소에 보관하지 않는다.

```bash
# CodeQL default setup: state=configured, languages java-kotlin·actions, query suite default, threat model remote
gh api -X PATCH repos/lilamaris/lilboard/code-scanning/default-setup --input cq.json
```

run `39482226154`의 `Analyze (java-kotlin)`·`Analyze (actions)` job 모두 성공. 언어 설정은 `java-kotlin`·`actions`, 첫 스캔 결과 java-kotlin rules 76·results 0, actions rules 17·results 0.

## 검증 메모

- GitHub 보안 기능은 저장소 공개 범위·소유 형태·요금제에 따라 사용 가능 여부가 다르다. 비공개 저장소의 CodeQL·dependency review는 Code Security 등 사용 조건을 먼저 확인한다. secret scanning·push protection은 별도로 확인하며, 단순히 Security 탭을 활성화하는 것으로 완료 처리하지 않는다. [CodeQL 사용 조건](https://docs.github.com/en/code-security/reference/code-scanning/troubleshoot-analysis-errors/private-repository-enablement), [Dependency review 사용 조건](https://docs.github.com/en/code-security/concepts/supply-chain-security/dependency-review)
- push protection은 합성 값으로 검증되지 않는다(2026-10-21, 4회 시도·alert 0). GitHub PAT는 GitHub이 실 존재를 검증하고, 합성 값은 차단되지 않는다. 개인 저장소에는 org test mode가 없어 확인 조건은 실 토큰 push 후 revoke뿐이다.
