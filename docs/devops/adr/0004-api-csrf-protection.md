# API 요청별 CSRF 보호

- 상태: 확정
- 일자: 2026-10-30

## 배경

PR #28의 CodeQL 검사는 두 서비스의 CSRF 전역 비활성화를 high 경고로 보고했다.
API는 세션·쿠키 인증이나 HTTP Basic을 사용하지 않으며, 회원은 Authorization의
Bearer JWT를 명시적으로 전송한다. 비로그인 회원가입·로그인은 JSON 요청을 사용한다.

## 결정

- CSRF 기본 보호를 활성화한다.
- 두 서비스는 Content-Type의 미디어 타입이 application/json인 요청만 CSRF 예외로 처리한다.
  charset 매개변수는 허용하며 Accept 헤더를 예외 판정에 사용하지 않는다.
- board는 명시적인 Authorization: Bearer 헤더가 있는 요청도 예외로 처리한다.
  실제 JWT의 유효성과 회원 권한은 기존 Resource Server와 접근 정책에서 검증한다.
- 브라우저 폼 및 text/plain 변경 요청에는 기본 CSRF 검사를 유지한다.
- 비로그인 시딩 호출도 Content-Type: application/json을 명시한다.
- 세션·쿠키 인증이나 CORS 정책을 추가할 때 이 예외 조건을 다시 검토한다.

## 근거

[Spring Security CSRF 문서](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html)는
기본 보호와 선택적인 요청 제외를 제공하며 JSON 요청의 content type 검증을 설명한다.
검증 결과는 [CodeQL 조치 기록](../records/github-security.md#pr-28-csrf-경고-조치)에 남긴다.
