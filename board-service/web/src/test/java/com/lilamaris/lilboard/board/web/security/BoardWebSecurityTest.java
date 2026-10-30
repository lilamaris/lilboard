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
        mvc.perform(post("/api/boards")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("회원은 게시판을 생성할 수 있다")
    void allowsMemberWrite() throws Exception {
        mvc.perform(post("/api/boards").with(user("member").roles("MEMBER")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("회원 권한이 없는 사용자의 생성 요청은 403으로 거부된다")
    void rejectsNonMemberWrite() throws Exception {
        mvc.perform(post("/api/boards").with(user("guest").roles("GUEST")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("명시되지 않은 경로는 회원에게도 허용하지 않는다")
    void deniesUnlistedEndpoint() throws Exception {
        mvc.perform(get("/api/private").with(user("member").roles("MEMBER")))
                .andExpect(status().isForbidden());
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
