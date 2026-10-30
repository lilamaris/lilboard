package com.lilamaris.lilboard.identity.launcher;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import com.lilamaris.lilboard.identity.jpa.repository.AccountRepository;
import java.net.URI;
import java.net.http.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CredentialApiIntegrationTest {
    @LocalServerPort int port;
    @Autowired AccountRepository accounts;
    @Autowired PasswordEncoder encoder;

    @BeforeEach
    void cleanAccounts() {
        accounts.deleteAll();
    }

    @Test
    @DisplayName("회원가입하면 비밀번호를 해시로 저장하고 사용자 ID만 반환한다")
    void registersWithHashedPassword() throws Exception {
        var response = post("register", "user@example.com", "password123");
        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(accounts.findAll())
                .singleElement()
                .satisfies(
                        account -> {
                            assertThat(encoder.matches("password123", account.getPasswordHash()))
                                    .isTrue();
                            assertThat(response.body())
                                    .contains(account.getId().toString())
                                    .doesNotContain("password");
                        });
    }

    @Test
    @DisplayName("정규화한 이메일이 중복되면 409를 반환한다")
    void rejectsDuplicateEmail() throws Exception {
        post("register", "User@Example.com", "password123");
        assertThat(post("register", "user@example.com", "password123").statusCode()).isEqualTo(409);
        assertThat(accounts.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("올바른 인증 정보로 로그인하면 서명과 발급자와 대상이 유효한 회원 JWT를 반환한다")
    void logsInAndIssuesMemberToken() throws Exception {
        post("register", "user@example.com", "password123");
        var response = post("login", "USER@example.com", "password123");
        assertThat(response.statusCode()).isEqualTo(200);
        var body = JsonPath.parse(response.body());
        var decoder =
                NimbusJwtDecoder.withJwkSetUri(
                                "http://localhost:" + port + "/.well-known/jwks.json")
                        .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer("http://localhost:8081"));
        var jwt = decoder.decode(body.read("$.accessToken", String.class));
        assertThat(jwt.getSubject()).isEqualTo(accounts.findAll().getFirst().getId().toString());
        assertThat(jwt.getAudience()).containsExactly("lilboard-api");
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("ROLE_MEMBER");
        assertThat(jwt.getExpiresAt()).isEqualTo(jwt.getIssuedAt().plusSeconds(900));
        assertThat(body.read("$.tokenType", String.class)).isEqualTo("Bearer");
    }

    @Test
    @DisplayName("비밀번호가 틀리면 토큰을 발급하지 않고 401을 반환한다")
    void rejectsWrongPassword() throws Exception {
        post("register", "user@example.com", "password123");
        var response = post("login", "user@example.com", "wrongpassword");
        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.body()).doesNotContain("accessToken");
    }

    @Test
    @DisplayName("등록되지 않은 이메일로 로그인하면 401을 반환한다")
    void rejectsUnknownEmail() throws Exception {
        assertThat(post("login", "missing@example.com", "password123").statusCode()).isEqualTo(401);
    }

    @Test
    @DisplayName("이메일 형식이 잘못되면 계정을 저장하지 않고 400을 반환한다")
    void rejectsInvalidEmail() throws Exception {
        assertThat(post("register", "invalid", "password123").statusCode()).isEqualTo(400);
        assertThat(accounts.count()).isZero();
    }

    @Test
    @DisplayName("비밀번호가 최소 길이보다 짧으면 400을 반환한다")
    void rejectsShortPassword() throws Exception {
        assertThat(post("register", "user@example.com", "short").statusCode()).isEqualTo(400);
    }

    @Test
    @DisplayName("공개 JWKS에는 개인키 정보가 포함되지 않는다")
    void exposesOnlyPublicKey() throws Exception {
        try (var client = HttpClient.newHttpClient()) {
            var response =
                    client.send(
                            HttpRequest.newBuilder(
                                            URI.create(
                                                    "http://localhost:"
                                                            + port
                                                            + "/.well-known/jwks.json"))
                                    .GET()
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(200);
            var keys = com.nimbusds.jose.jwk.JWKSet.parse(response.body());
            assertThat(keys.getKeys())
                    .singleElement()
                    .satisfies(jwk -> assertThat(jwk.isPrivate()).isFalse());
        }
    }

    private HttpResponse<String> post(String action, String email, String password)
            throws Exception {
        try (var client = HttpClient.newHttpClient()) {
            return client.send(
                    HttpRequest.newBuilder(
                                    URI.create("http://localhost:" + port + "/api/auth/" + action))
                            .header("Content-Type", "application/json")
                            .POST(
                                    HttpRequest.BodyPublishers.ofString(
                                            "{\"email\":\""
                                                    + email
                                                    + "\",\"password\":\""
                                                    + password
                                                    + "\"}"))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
        }
    }
}
