package com.lilamaris.lilboard.board.web.security;

import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@SpringJUnitWebConfig(BoardWebSecurityTest.TestApplication.class)
class BoardWebSecurityTest {
    @Autowired WebApplicationContext context;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    @DisplayName("비로그인 사용자는 공개 목록을 조회할 수 있다")
    void allowsPublicRead() throws Exception {
        mvc.perform(get("/api/boards")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("비로그인 사용자의 생성 요청은 401로 거부된다")
    void rejectsAnonymousWrite() throws Exception {
        mvc.perform(post("/api/boards").contentType("application/json"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("회원은 게시판을 생성할 수 있다")
    void allowsMemberWrite() throws Exception {
        mvc.perform(
                        post("/api/boards")
                                .contentType("application/json")
                                .with(user("member").roles("MEMBER")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("회원 권한이 없는 사용자의 생성 요청은 403으로 거부된다")
    void rejectsNonMemberWrite() throws Exception {
        mvc.perform(
                        post("/api/boards")
                                .contentType("application/json")
                                .with(user("guest").roles("GUEST")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("명시되지 않은 경로는 회원에게도 허용하지 않는다")
    void deniesUnlistedEndpoint() throws Exception {
        mvc.perform(get("/api/private").with(user("member").roles("MEMBER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("회원의 브라우저 폼 요청도 CSRF 토큰이 없으면 차단한다")
    void rejectsFormWriteWithoutCsrfToken() throws Exception {
        mvc.perform(
                        post("/api/boards")
                                .contentType("application/x-www-form-urlencoded")
                                .with(user("member").roles("MEMBER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("text/plain 요청은 CSRF 예외를 적용하지 않는다")
    void rejectsPlainTextWriteWithoutCsrfToken() throws Exception {
        mvc.perform(
                        post("/api/boards")
                                .contentType("text/plain")
                                .with(user("member").roles("MEMBER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("문자셋이 포함된 JSON 요청은 별도 CSRF 토큰 없이 회원 정책을 적용한다")
    void acceptsJsonWithCharset() throws Exception {
        mvc.perform(
                        post("/api/boards")
                                .contentType("application/json;charset=UTF-8")
                                .with(user("member").roles("MEMBER")))
                .andExpect(status().isOk());
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import({BoardSecurityConfiguration.class, PublicController.class})
    static class TestApplication {
        @Bean
        JwtDecoder jwtDecoder() {
            return mock(JwtDecoder.class);
        }
    }

    @RestController
    static class PublicController {
        @GetMapping("/api/boards")
        String list() {
            return "[]";
        }

        @PostMapping("/api/boards")
        String create() {
            return "created";
        }
    }
}
