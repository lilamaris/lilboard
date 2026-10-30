# 이메일·비밀번호 인증과 공개 조회

`identity-service`는 회원가입과 이메일·비밀번호 검증 후 RS256 액세스 JWT를 발급한다.
`board-service`는 인증 서비스의 JWKS 공개키로 서명과 만료, 발급자(`iss`), 대상(`aud`)을 검증한다.
비로그인 조회는 사용자 계정이나 토큰을 생성하지 않는다.

## API

| 서비스 | 메서드·경로 | 접근 | 결과 |
| --- | --- | --- | --- |
| identity (8081) | `POST /api/auth/register` | 공개 | 사용자 ID, 201 |
| identity (8081) | `POST /api/auth/login` | 공개 | 액세스 토큰, 200 |
| identity (8081) | `GET /.well-known/jwks.json` | 공개 | 서명 검증용 공개키 |
| board (8080) | `GET /api/boards`, `GET /api/boards/{id}` | 공개 | 게시판 조회 |
| board (8080) | `GET /api/posts`, `GET /api/posts/{id}` | 공개 | 게시글 조회 |
| board (8080) | 기존 게시판·게시글 POST·PUT 및 게시판 DELETE | 회원 | 기존 변경 API |

기존 health·metrics·prometheus 및 OpenAPI·Swagger 조회 경로도 공개한다.
명시하지 않은 경로는 기본 차단한다. 공개 조회라도 잘못되거나 만료된 Bearer 토큰을
함께 보내면 401을 반환한다. 비로그인 상태에서는 Authorization 헤더를 생략한다.

가입·로그인 요청 형식은 동일하다.

```json
{"email":"user@example.com","password":"password123"}
```

이메일은 소문자로 정규화하며 최대 254자, 비밀번호는 8~128자다.
비밀번호는 PBKDF2 해시로 저장한다. 중복 이메일은 409, 일치하지 않는 인증 정보는 401,
유효하지 않은 입력은 400을 반환한다. 가입 응답에는 비밀번호나 해시를 포함하지 않는다.

로그인 응답:

```json
{"accessToken":"<JWT>","tokenType":"Bearer","expiresIn":900}
```

변경 API 요청에는 `Authorization: Bearer <JWT>`를 전달한다.
JWT의 `sub`는 이메일과 독립적인 사용자 UUID이며 `roles`는 `ROLE_MEMBER`,
`aud`는 `lilboard-api`다. 회원 권한이 없는 유효한 JWT로 변경하면 403을 반환한다.

## 실행 설정

인증 서비스는 게시판 서비스와 별도 DB 또는 별도 스키마를 사용해야 한다.
Flyway 마이그레이션 이력을 공유하지 않는다. DB·스키마와 사용자는 실행 전에 준비한다.

| 변수 | 서비스 | 값 |
| --- | --- | --- |
| `IDENTITY_DB_URL` | identity | 인증용 PostgreSQL JDBC URL |
| `IDENTITY_DB_USERNAME` | identity | 인증 DB 사용자 |
| `IDENTITY_DB_PASSWORD` | identity | 인증 DB 비밀번호 |
| `JWT_PRIVATE_KEY` | identity | PKCS#8 PEM 개인키 Resource, 예: `file:/secure/identity-private.pem` |
| `JWT_PUBLIC_KEY` | identity | X.509 PEM 공개키 Resource, 예: `file:/secure/identity-public.pem` |
| `JWT_ISSUER` | 양쪽 | 동일한 발급자 식별자. 기본값 `http://localhost:8081` |
| `JWT_JWK_SET_URI` | board | board에서 접근 가능한 JWKS URL. 기본값 `http://localhost:8081/.well-known/jwks.json` |

키 생성 예시:

```sh
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:3072 -out identity-private.pem
openssl pkey -in identity-private.pem -pubout -out identity-public.pem
```

개인키는 저장소에 추가하지 않고 인증 서비스만 읽을 수 있도록 관리한다.
`src/test/resources/test-*.pem`은 자동 테스트 전용 키이며 실행 환경에 사용하지 않는다.
키와 DB 환경 변수를 설정한 후 각 프로세스를 실행한다.

```sh
./gradlew :identity-service:launcher:bootRun
./gradlew :board-service:launcher:bootRun --args='--spring.profiles.active=prod'
```

board의 기존 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` 설정도 필요하다.
서비스 간 호출 주소는 실제 배치에 맞게 설정하고 외부 통신에는 HTTPS를 사용한다.

## 현재 범위

액세스 토큰은 15분간 유효하며 만료 후 다시 로그인한다.
갱신 토큰, 서버 측 즉시 로그아웃·토큰 폐기, 이메일 소유권 확인, 비밀번호 재설정,
게스트 계정과 요청 횟수 제한은 포함하지 않는다.

변경 API는 회원 여부를 검사한다. 현재 게시판·게시글 모델에 작성자가 없으므로
모든 회원이 기존 변경 API를 사용할 수 있다. 작성자 본인만 수정·삭제하도록 하는
소유권 검사와 게시판 관리자의 권한 분리는 별도 기능이다.

## 모듈 책임

`launcher`는 실행 진입점과 모듈 조립을 담당한다. HTTP 컨트롤러, URL 접근 정책,
인증 실패 응답, JWT 발급·검증 설정은 각 서비스의 `web` 모듈에 둔다.
`identity-service:application`의 Clock·사용자 ID 생성 설정은 application에서 제공한다.
Security와 WebMVC 운영 의존성은 web에 선언하며 launcher의 보안 의존성은 통합 테스트용이다.

현재 HTTP 보안은 URL·메서드·컨트롤러 계약과 함께 변경되므로 WebMVC와 동일한
모듈에서 관리하고 `controller`와 `security` 패키지로 역할을 구분한다.
별도 security 모듈은 여러 HTTP 어댑터가 독립적으로 재사용할 구현이 생기거나,
별도 배포·의존성 경계가 필요해질 때 검토한다. Servlet 기반 필터 설정은
WebFlux에 그대로 재사용할 수 없다.

이 선택과 application의 프레임워크 독립성은 별개다. 현재 CredentialService는
Spring Security의 UserDetailsService·PasswordEncoder에 의존한다.
추후 프레임워크 독립성이 필요하면 이 연동을 web 어댑터와 application 포트로
분리할 수 있으며, 이를 위해 별도 Gradle security 모듈이 반드시 필요한 것은 아니다.

인증 컨트롤러는 RegisterAccountUseCase·LoginUseCase를 호출하고, command/result로
application과 통신한다. 로그인 유스케이스는 CredentialAuthenticatePort와
AccessTokenIssuePort를 사용하며, Spring Security 인증과 JWT 서명 구현은 web의
보안 어댑터가 담당한다. HTTP 응답의 Bearer 표기는 LoginResponse에서 구성한다.
인증 API의 OpenAPI 문서는 `/v3/api-docs`, Swagger UI는 `/swagger-ui.html`에서
비로그인 상태로 조회할 수 있다.
