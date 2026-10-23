# 빌드와 품질 CI

[DevOps 체크리스트](../checklist.md)의 상세 설정과 검증 근거를 기록한다. 날짜·커밋·실행 결과는 기존 확인 시점의 기록이며, 문서 분리 과정에서 원격 설정이나 런타임을 재검증하지 않았다.

## 포맷 검사

대상: Spotless 포맷 검사 + `.editorconfig`·`.gitattributes` (BUILD 단계에 포함)

`35f2ab2`

## Gradle 실행 설정

대상: Gradle 실행 설정: 캐시·configuration cache·parallel·jvmargs (`gradle.properties`)

`bb507a8`

## 계층 경계

대상: ArchUnit 계층 경계 검증(domain → 상위 계층·Spring 금지, application → 어댑터 금지, kernel → board-service 금지)

`f52022e`

## CI 실행 경로

대상: CI 트리거(main push + PR + 수동 실행)와 lint job 분리

`0af2451`

## 플랫폼 설정 확인 기록

2026-10-21 확인·적용. 저장소 public, account `lilamaris`. GitHub Settings 영역의 기존 기록이며 문서 분리 시 원격 설정을 재조회하지 않았다.

| 항목 | 상태 | 근거 |
|---|---|---|
| CI | green | run #5 push(main) success, run #3·#4는 PR #1·#2 각 1회 → 중복 실행 없음 |
