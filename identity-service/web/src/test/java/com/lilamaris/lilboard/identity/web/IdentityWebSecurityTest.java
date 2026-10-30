package com.lilamaris.lilboard.identity.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.jayway.jsonpath.JsonPath;
import com.lilamaris.lilboard.identity.application.CredentialService;
import com.lilamaris.lilboard.identity.application.exception.EmailAlreadyRegisteredException;
import com.lilamaris.lilboard.identity.application.port.in.command.RegisterAccountCommand;
import com.lilamaris.lilboard.identity.application.port.in.result.RegisterAccountResult;
import com.lilamaris.lilboard.identity.application.service.LoginService;
import com.lilamaris.lilboard.identity.web.controller.AuthController;
import com.lilamaris.lilboard.identity.web.controller.JwkSetController;
import com.lilamaris.lilboard.identity.web.security.CredentialAuthenticationAdapter;
import com.lilamaris.lilboard.identity.web.security.IdentitySecurityConfiguration;
import com.lilamaris.lilboard.identity.web.security.JwtAccessTokenAdapter;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import java.time.*;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
        classes = IdentityWebSecurityTest.TestApplication.class,
        properties = {
            "auth.jwt.public-key=classpath:test-public.pem",
                    "auth.jwt.private-key=classpath:test-private.pem",
            "auth.jwt.issuer=http://identity.test", "auth.jwt.access-token-ttl=PT15M"
        })
@AutoConfigureMockMvc
class IdentityWebSecurityTest {
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    @Autowired MockMvc mvc;
    @Autowired PasswordEncoder passwords;
    @MockitoBean CredentialService credentials;

    @Test
    @DisplayName("비로그인 회원가입 요청은 인증 유스케이스의 사용자 ID와 201을 반환한다")
    void registersWithoutAuthentication() throws Exception {
        when(credentials.register(new RegisterAccountCommand("user@example.com", "password123")))
                .thenReturn(new RegisterAccountResult(USER_ID));
        mvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body("password123")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(USER_ID.toString()));
    }

    @Test
    @DisplayName("중복 이메일 예외는 HTTP 409로 변환된다")
    void reportsDuplicateEmail() throws Exception {
        when(credentials.register(new RegisterAccountCommand("user@example.com", "password123")))
                .thenThrow(new EmailAlreadyRegisteredException());
        mvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body("password123")))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("유효하지 않은 비밀번호는 HTTP 400으로 거부된다")
    void rejectsInvalidRequest() throws Exception {
        mvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body("short")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("올바른 비밀번호로 로그인하면 공개 JWKS로 검증 가능한 회원 JWT를 발급한다")
    void issuesTokenVerifiableWithPublishedKey() throws Exception {
        when(credentials.loadUserByUsername("user@example.com"))
                .thenReturn(
                        User.withUsername(USER_ID.toString())
                                .password(passwords.encode("password123"))
                                .roles("MEMBER")
                                .build());
        var response =
                mvc.perform(
                                post("/api/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(body("password123")))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse();
        String token = JsonPath.read(response.getContentAsString(), "$.accessToken");
        String jwks =
                mvc.perform(get("/.well-known/jwks.json"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        var key = (RSAKey) JWKSet.parse(jwks).getKeys().getFirst();
        assertThat(key.isPrivate()).isFalse();
        var decoder = NimbusJwtDecoder.withPublicKey(key.toRSAPublicKey()).build();
        decoder.setJwtValidator(new JwtIssuerValidator("http://identity.test"));
        var jwt = decoder.decode(token);
        assertThat(jwt.getSubject()).isEqualTo(USER_ID.toString());
        assertThat(jwt.getIssuedAt()).isEqualTo(NOW);
        assertThat(jwt.getExpiresAt()).isEqualTo(NOW.plusSeconds(900));
        assertThat(jwt.getAudience()).containsExactly("lilboard-api");
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("ROLE_MEMBER");
    }

    @Test
    @DisplayName("틀린 비밀번호는 HTTP 401로 거부된다")
    void rejectsWrongPassword() throws Exception {
        when(credentials.loadUserByUsername("user@example.com"))
                .thenReturn(
                        User.withUsername(USER_ID.toString())
                                .password(passwords.encode("password123"))
                                .roles("MEMBER")
                                .build());
        mvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body("wrongpassword")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("없는 이메일로 로그인하면 HTTP 401을 반환한다")
    void rejectsUnknownAccount() throws Exception {
        when(credentials.loadUserByUsername("user@example.com"))
                .thenThrow(new UsernameNotFoundException("missing"));
        mvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body("password123")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("등록되지 않은 경로는 비로그인 요청에서 차단한다")
    void deniesUnlistedEndpoint() throws Exception {
        mvc.perform(get("/api/private")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("OpenAPI 문서는 인증 없이 조회할 수 있고 회원가입과 로그인 응답 계약을 제공한다")
    void exposesAuthenticationApiDocumentation() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/auth/register'].post.summary").value("회원가입"))
                .andExpect(
                        jsonPath(
                                        "$.paths['/api/auth/register'].post.responses['201'].content['application/json'].schema['$ref']")
                                .value("#/components/schemas/RegisterAccountResult"))
                .andExpect(
                        jsonPath(
                                        "$.paths['/api/auth/login'].post.responses['200'].content['application/json'].schema['$ref']")
                                .value("#/components/schemas/LoginResponse"))
                .andExpect(jsonPath("$.paths['/api/auth/login'].post.responses['401']").exists());
    }

    @Test
    @DisplayName("CSRF 토큰이 없는 브라우저 폼 로그인 요청은 403으로 차단된다")
    void rejectsFormLoginWithoutCsrfToken() throws Exception {
        mvc.perform(
                        post("/api/auth/login")
                                .contentType("application/x-www-form-urlencoded")
                                .param("email", "user@example.com")
                                .param("password", "password123"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("JSON 본문을 text/plain으로 보낸 회원가입 요청은 403으로 차단된다")
    void rejectsPlainTextRegistrationWithoutCsrfToken() throws Exception {
        mvc.perform(
                        post("/api/auth/register")
                                .contentType("text/plain")
                                .content(body("password123")))
                .andExpect(status().isForbidden());
    }

    private String body(String password) {
        return "{\"email\":\"user@example.com\",\"password\":\"" + password + "\"}";
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({
        IdentitySecurityConfiguration.class,
        AuthController.class,
        JwkSetController.class,
        JwtAccessTokenAdapter.class,
        LoginService.class,
        CredentialAuthenticationAdapter.class,
        AuthExceptionHandler.class
    })
    static class TestApplication {
        @Bean
        Clock clock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }
}
