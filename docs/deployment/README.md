# 배포와 실행

프로젝트를 빌드·배포·실행하는 데 필요한 구성 설명과 사용 방법을 이 디렉터리에서 관리한다. 공통 이미지 구성, 환경별 실행 절차, 향후 이미지 게시·원격 배포 절차를 각 문서로 구분한다.

| 문서 | 내용 |
|---|---|
| [컨테이너 이미지 구성과 빌드](container-image.md) | 베이스 이미지, 서비스별 Dockerfile, 빌드 컨텍스트, `.dockerignore`, 공통 실행 설정 |
| [Docker Compose로 실행](compose.md) | 앱·PostgreSQL 실행 구성, 환경 변수, 소스 빌드·기존 이미지 사용, 데이터 관리 |
| [JVM에서 직접 실행](jvm-runtime.md) | 프로파일·DB 환경 변수 설정과 Gradle `bootRun` 실행 |
| [로컬 실행 예시](local-runtime.md) | PostgreSQL·앱 수동 실행, 기동 확인, 재기동, 종료와 데이터 삭제 |

현재 Dockerfile·`.dockerignore`·Compose·`.env.example`이 구성되어 있다. 실행 스크립트, 이미지 레지스트리 게시와 원격 배포는 후속 작업이며, 관련 문서는 구성에 맞춰 여기에 추가한다.

- 진행 상태와 실제 검증 근거: [DevOps 체크리스트](../devops-checklist.md)
- 배포 대상·레지스트리·Runner·시크릿 등 정책 결정: [ADR](../adr/)

파일 구성 완료와 실제 동작 확인은 체크리스트에서 구분한다. 문서의 사용 예시만으로 해당 동작의 검증 완료를 의미하지 않는다.
