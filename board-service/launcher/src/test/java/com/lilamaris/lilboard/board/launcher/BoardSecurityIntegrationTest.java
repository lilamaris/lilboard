package com.lilamaris.lilboard.board.launcher;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.*;
import java.time.Instant;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestMemberAuthentication.class)
class BoardSecurityIntegrationTest {
    @LocalServerPort int port;

    @Test
    @DisplayName("비로그인 사용자는 게시판 목록을 조회할 수 있다")
    void allowsAnonymousPublicRead() throws Exception {
        assertThat(send("GET", "/api/boards", null)).isEqualTo(200);
    }

    @Test
    @DisplayName("비로그인 사용자는 게시글 목록을 조회할 수 있다")
    void allowsAnonymousPostRead() throws Exception {
        assertThat(send("GET", "/api/posts", null)).isEqualTo(200);
    }

    @Test
    @DisplayName("비로그인 사용자의 게시판 생성 요청은 401로 거부된다")
    void rejectsAnonymousBoardWrite() throws Exception {
        assertThat(send("POST", "/api/boards", null)).isEqualTo(401);
    }

    @Test
    @DisplayName("비로그인 사용자의 게시글 생성 요청은 401로 거부된다")
    void rejectsAnonymousPostWrite() throws Exception {
        assertThat(send("POST", "/api/posts", null)).isEqualTo(401);
    }

    @Test
    @DisplayName("비로그인 사용자의 수정 요청은 401로 거부된다")
    void rejectsAnonymousUpdate() throws Exception {
        assertThat(send("PUT", "/api/posts/00000000-0000-0000-0000-000000000001", null))
                .isEqualTo(401);
    }

    @Test
    @DisplayName("비로그인 사용자의 삭제 요청은 401로 거부된다")
    void rejectsAnonymousDelete() throws Exception {
        assertThat(send("DELETE", "/api/boards/00000000-0000-0000-0000-000000000001", null))
                .isEqualTo(401);
    }

    @Test
    @DisplayName("잘못된 JWT를 보내면 공개 조회도 401로 거부된다")
    void rejectsMalformedToken() throws Exception {
        assertThat(send("GET", "/api/posts", "invalid")).isEqualTo(401);
    }

    @Test
    @DisplayName("만료된 JWT는 401로 거부된다")
    void rejectsExpiredToken() throws Exception {
        assertThat(
                        send(
                                "GET",
                                "/api/posts",
                                token(
                                        "ROLE_MEMBER",
                                        "http://localhost:8081",
                                        "lilboard-api",
                                        Instant.now().minusSeconds(300))))
                .isEqualTo(401);
    }

    @Test
    @DisplayName("발급자가 다른 JWT는 401로 거부된다")
    void rejectsWrongIssuer() throws Exception {
        assertThat(
                        send(
                                "GET",
                                "/api/posts",
                                token(
                                        "ROLE_MEMBER",
                                        "other",
                                        "lilboard-api",
                                        Instant.now().plusSeconds(300))))
                .isEqualTo(401);
    }

    @Test
    @DisplayName("대상이 다른 JWT는 401로 거부된다")
    void rejectsWrongAudience() throws Exception {
        assertThat(
                        send(
                                "GET",
                                "/api/posts",
                                token(
                                        "ROLE_MEMBER",
                                        "http://localhost:8081",
                                        "other",
                                        Instant.now().plusSeconds(300))))
                .isEqualTo(401);
    }

    @Test
    @DisplayName("회원 권한이 없는 JWT로 게시글을 생성하면 403으로 거부된다")
    void rejectsNonMemberWrite() throws Exception {
        assertThat(
                        send(
                                "POST",
                                "/api/posts",
                                token(
                                        "ROLE_GUEST",
                                        "http://localhost:8081",
                                        "lilboard-api",
                                        Instant.now().plusSeconds(300))))
                .isEqualTo(403);
    }

    @Test
    @DisplayName("정의되지 않은 경로는 회원에게도 허용하지 않는다")
    void rejectsUnlistedEndpoint() throws Exception {
        assertThat(
                        send(
                                "GET",
                                "/api/private",
                                token(
                                        "ROLE_MEMBER",
                                        "http://localhost:8081",
                                        "lilboard-api",
                                        Instant.now().plusSeconds(300))))
                .isEqualTo(403);
    }

    private String token(String role, String issuer, String audience, Instant expiresAt)
            throws Exception {
        return TestMemberAuthentication.token(role, issuer, audience, expiresAt);
    }

    private int send(String method, String path, String token) throws Exception {
        try (var client = HttpClient.newHttpClient()) {
            var request =
                    HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                            .header("Content-Type", "application/json")
                            .method(method, HttpRequest.BodyPublishers.ofString("{}"));
            if (token != null) request.header("Authorization", "Bearer " + token);
            return client.send(request.build(), HttpResponse.BodyHandlers.ofString()).statusCode();
        }
    }
}
