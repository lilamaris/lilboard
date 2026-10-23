# DevOps 문서 구조 검증 기록

[체크리스트](../checklist.md#문서-관리)에서 이 작업의 진행 상태를 관리한다.

## 변경 범위

- 작업 일자: 2026-10-23 (Asia/Seoul).
- 대상: `2d22349` 기반 `docs/devops-documentation` 브랜치의 미커밋 문서 변경.
- 별도 worktree에서 작업하며, 기존 개발 브랜치의 파일은 수정하지 않는다.
- 체크리스트, 배포 안내, DevOps ADR을 `docs/devops/`로 모으고 상세 설정·검증 기록을 `records/`의 항목별 문서로 분리했다.
- README와 AGENTS.md의 링크·문서 갱신 지침을 새 경로에 맞췄다.
- 기존 확인 일자·커밋·실행 결과·미확인 상태를 보존한다. 이 문서 정리는 GitHub 설정이나 런타임의 재검증을 의미하지 않는다.

## 검증

- 2026-10-23 로컬 검증. 일회성 Python 검사로 `origin/main`의 원본 체크리스트(`2d22349`)와 분리된 문서를 대조했다. 기존 64개 항목의 순서·상태·상세 근거와 날짜·커밋·실행 ID·이미지 digest를 모두 보존했음을 확인했다.
- README·AGENTS.md와 `docs/`의 Markdown 20개 문서에서 로컬 링크·앵커 122개가 정상이며, 이전 DevOps 문서 경로 참조가 없음을 확인했다.
- 배포 안내의 기존 명령 블록과 launcher 커버리지 ADR 본문이 이동 전과 동일함을 확인했다.
- worktree 루트에서 `./gradlew spotlessApply spotlessCheck --console=plain` 성공. `git diff --check` 통과. 변경 파일은 Markdown뿐이다.

Java 코드·Gradle 설정·workflow·배포 구성은 변경하지 않아 단위·통합 테스트, actionlint, 이미지 빌드·Compose 기동과 원격 CI는 실행하지 않는다.
