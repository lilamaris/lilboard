# loadtest 시딩 API의 보안 확장

- 상태: 확정
- 일자: 2026-10-30

## 배경

JWT 접근 제어의 기본 차단 정책이 loadtest 전용 시딩 API에도 적용된다.
시딩 API는 비로그인 호출을 지원하되, 기본 web 보안 설정이 seed 경로나
loadtest 프로파일을 직접 알아야 하는 결합은 피한다.

## 결정

- web은 HTTP 접근 규칙을 추가하는 `HttpAuthorizationCustomizer` 확장점을 제공한다.
- seed는 `loadtest`에서만 확장 빈을 등록하고 정확히 `POST /api/seed`를 공개한다.
- 확장 규칙은 기본 규칙 뒤, 최종 `anyRequest().denyAll()` 앞에 적용한다.
- 별도 SecurityFilterChain이나 전역 프로파일 분기로 기본 정책을 대체하지 않는다.
- loadtest가 없으면 확장 빈과 시딩 컨트롤러가 모두 없고, 기본 차단을 유지한다.
- seed는 web에 의존하며 web은 seed를 참조하지 않는다.

## 검증

프로파일별 빈 등록, 비로그인 시딩 성공, 다른 메서드·하위 경로 차단,
일반 회원 API 정책 유지, 비활성 프로파일의 비로그인·회원 요청 차단을 검증한다.
실행 근거와 한계는 [시딩 기록](../records/load-test-seeding.md#프로파일별-보안-확장)에 남긴다.
