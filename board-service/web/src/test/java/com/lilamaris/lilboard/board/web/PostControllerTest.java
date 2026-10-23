package com.lilamaris.lilboard.board.web;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.lilamaris.lilboard.board.application.port.in.CreatePostUseCase;
import com.lilamaris.lilboard.board.application.port.in.command.CreatePostCommand;
import com.lilamaris.lilboard.board.application.port.in.result.CreatePostResult;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PostControllerTest {
    private CreatePostUseCase useCase;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        useCase = mock(CreatePostUseCase.class);
        mvc = MockMvcBuilders.standaloneSetup(new PostController(useCase)).build();
    }

    @Test
    @DisplayName("유효한 생성 요청이면 유스케이스 결과와 201을 반환한다")
    void createsPost() throws Exception {
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID boardId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        Instant time = Instant.parse("2026-01-01T00:00:00Z");
        when(useCase.create(new CreatePostCommand(boardId, "게시글 제목", "본문")))
                .thenReturn(new CreatePostResult(id, boardId, "게시글 제목", "본문", time));

        mvc.perform(
                        post("/api/posts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"boardId\":\""
                                                + boardId
                                                + "\",\"title\":\"게시글 제목\",\"content\":\"본문\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.boardId").value(boardId.toString()))
                .andExpect(jsonPath("$.title").value("게시글 제목"))
                .andExpect(jsonPath("$.content").value("본문"))
                .andExpect(jsonPath("$.createdAt").value(time.toString()));
    }

    @Test
    @DisplayName("제목 300자와 본문 50000자의 경계값이면 생성에 성공한다")
    void acceptsMaximumLengths() throws Exception {
        UUID boardId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        String title = "a".repeat(300);
        String content = "b".repeat(50000);
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");
        when(useCase.create(new CreatePostCommand(boardId, title, content)))
                .thenReturn(
                        new CreatePostResult(
                                id,
                                boardId,
                                title,
                                content,
                                Instant.parse("2026-01-01T00:00:00Z")));

        mvc.perform(
                        post("/api/posts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"boardId\":\""
                                                + boardId
                                                + "\",\"title\":\""
                                                + title
                                                + "\",\"content\":\""
                                                + content
                                                + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value(title))
                .andExpect(jsonPath("$.content").value(content));
    }

    @ParameterizedTest
    @MethodSource("invalidBodies")
    @DisplayName("필수 값 누락·공백·길이 초과 또는 잘못된 JSON이면 400을 반환하고 생성하지 않는다")
    void rejectsInvalidBody(String body) throws Exception {
        mvc.perform(post("/api/posts").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(useCase);
    }

    static Stream<String> invalidBodies() {
        return Stream.of(
                "{}",
                "{\"boardId\":null,\"title\":\"제목\",\"content\":\"본문\"}",
                "{\"boardId\":\"00000000-0000-0000-0000-000000000002\",\"title\":\" \",\"content\":\"본문\"}",
                "{\"boardId\":\"00000000-0000-0000-0000-000000000002\",\"title\":\"제목\",\"content\":\" \"}",
                "{\"boardId\":\"00000000-0000-0000-0000-000000000002\",\"title\":null,\"content\":\"본문\"}",
                "{\"boardId\":\"00000000-0000-0000-0000-000000000002\",\"title\":\"제목\",\"content\":null}",
                "{\"boardId\":\"00000000-0000-0000-0000-000000000002\",\"title\":\""
                        + "a".repeat(301)
                        + "\",\"content\":\"본문\"}",
                "{\"boardId\":\"00000000-0000-0000-0000-000000000002\",\"title\":\"제목\",\"content\":\""
                        + "b".repeat(50001)
                        + "\"}",
                "{",
                "");
    }
}
