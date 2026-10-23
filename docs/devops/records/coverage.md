# 커버리지 보고서와 게이트

[DevOps 체크리스트](../checklist.md)의 상세 설정과 검증 근거를 기록한다. 날짜·커밋·실행 결과는 기존 확인 시점의 기록이며, 문서 분리 과정에서 원격 설정이나 런타임을 재검증하지 않았다.

## 보고서와 보관

대상: JaCoCo 커버리지 보고서(html·xml)와 CI artifact 보관(7일)

`c661870`

## 모듈별 게이트

대상: 모듈별 라인 커버리지 게이트(기본 80%, `coverage.minimum`·`coverage.excludes`)

`183cb8c` (예외 정책과 실패 검증은 P0에 별도 기록)

## Launcher 제외 정책

대상: launcher 커버리지 예외 정책 확정

실행 진입점·모듈 조립에 한정해 게이트 제외 유지. [정책 ADR](../adr/0001-launcher-coverage-policy.md) 참조(2026-10-21 확정)

## 게이트 동작 검증

대상: 커버리지 게이트 실패·제외 동작 검증

현재 launcher 제외 설정과 기준 변경 동작 확인. [커버리지 게이트 동작 검증 기록](#커버리지-게이트-동작-검증-기록) 참조

## Launcher 역할 제한

대상: launcher 역할 제한 ArchUnit 검증

운영 코드를 진입점·승인된 설정 클래스로 제한하고 허용하지 않은 클래스 추가 시 실패하는지 확인

## 검증 메모

- `check`와 `jacocoTestCoverageVerification`은 `java.base` 컨벤션에서 명시적으로 연결했다.
- JaCoCo violation rule의 `excludes`는 `element`가 지정한 대상의 이름에 적용된다. 기본 `element`는 `BUNDLE`이므로 현재 `*launcher*`는 번들 이름을 대상으로 한다. `CLASS` 규칙에서는 클래스 이름 패턴을 사용할 수 있다. 모듈 제외와 클래스 제외를 구분한다. [Gradle 공식 문서](https://docs.gradle.org/current/javadoc/org/gradle/testing/jacoco/tasks/rules/JacocoViolationRule.html)
- 기존 커버리지 메모(kernel·domain·jpa 100%, application 81.8%, launcher 33.3%)는 측정 커밋과 명령이 남아 있지 않아 참고값으로만 취급한다. 다음 측정부터 커밋 SHA·작업 트리 변경 여부·측정 일자·명령·보고서 위치를 함께 기록한다. 보고서 생성 명령은 `./gradlew jacocoTestReport`이며, 강제 재측정이 필요하면 `--rerun-tasks`를 사용한다.

## 커버리지 게이트 동작 검증 기록

| 구분 | 내용 |
|---|---|
| 검증 일자 | 2026-10-21 (Asia/Seoul) |
| 검증 대상 커밋 | `3c9afa62c95d95278f763feff42260658fe47fd7` |
| 실행 전 작업 트리 | 변경 없음. 실행 후 당시 체크리스트에 결과만 기록했다. |
| 기본 설정 | `coverage.minimum=0.80`, `coverage.excludes=*launcher*` |
| 보충 | 설정 파일은 수정하지 않고 `-P` 인자로 검증 조건을 변경했다. |

| 실행 명령 | 기대 결과 | 실제 결과 |
|---|---|---|
| `./gradlew build --rerun-tasks --console=plain` | 전체 테스트·기본 게이트 통과 | 성공, 52개 작업 실행. launcher의 낮은 커버리지는 현재 제외 설정에 따라 허용 |
| `./gradlew :board-service:launcher:check -Pcoverage.excludes= --console=plain` | launcher 포함 시 80% 미달로 실패 | 종료 코드 1, `jacocoTestCoverageVerification`에서 `0.33 < 0.80` 위반으로 실패 |
| `./gradlew :board-service:launcher:check -Pcoverage.excludes= -Pcoverage.minimum=0.30 --console=plain` | 동일 대상에서 기준을 30%로 낮추면 통과 | 성공 |

기준 미달 실패가 `check`까지 전파되고, 제외 설정과 기준값이 실제 결과에 반영됨을 확인했다.
launcher 제외 유지 정책은 [ADR](../adr/0001-launcher-coverage-policy.md)로 확정했다. 역할 제한 ArchUnit 검증은 별도 P0 작업으로 남아 있다.

전체 테스트 재실행 후 모듈별 `build/reports/jacoco/test/jacocoTestReport.xml`의 최상위 `LINE` 카운터를 측정했다.

| 모듈 | 실행된 라인 / 전체 라인 | 라인 커버리지 |
|---|---|---|
| kernel | 200 / 200 | 100% |
| domain | 11 / 11 | 100% |
| application | 12 / 12 | 100% |
| jpa | 1 / 1 | 100% |
| web | 4 / 4 | 100% |
| launcher | 1 / 3 | 33.3% |
