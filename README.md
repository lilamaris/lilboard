# lilboard

MSA(Microservices Architecture) 기반 게시판 서비스를 목표로 하는 Java·Gradle 멀티 모듈 프로젝트입니다.

## 목차

- [프로젝트 구조](#프로젝트-구조)
- [개발 환경](#개발-환경)
- [빌드 및 검증](#빌드-및-검증)
- [에이전트 작업 규칙](#에이전트-작업-규칙)

## 프로젝트 구조

```text
board-service/
  domain/       # 게시판 도메인
  application/  # 애플리케이션 계층
kernel/         # 공통 코드
build-logic/    # 공통 빌드·포맷 설정
```

## 개발 환경

- JDK 21
- Gradle Wrapper 사용 (`gradlew`, `gradlew.bat`)

## 빌드 및 검증

프로젝트 루트에서 실행합니다.

```bash
./gradlew build          # 포맷 검사·빌드·테스트
./gradlew spotlessApply  # 자동 포맷
./gradlew spotlessCheck  # 포맷 검사
```

모든 브랜치 push와 PR에서 GitHub Actions로 빌드와 검증을 수행합니다.

Dependabot은 매주 월요일 오전 9시(한국 시간)에 Gradle·GitHub Actions 업데이트를 확인합니다.
minor·patch 업데이트는 묶고, major 업데이트는 개별 PR로 생성합니다.

## 에이전트 작업 규칙

[AGENTS.md](AGENTS.md)는 AI 에이전트가 이 저장소에서 작업할 때 따르는 지침입니다.
코드 변경, 포맷, 테스트 및 커밋 규칙을 정의합니다.
