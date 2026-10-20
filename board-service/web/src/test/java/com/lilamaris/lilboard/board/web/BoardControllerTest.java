package com.lilamaris.lilboard.board.web;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.lilamaris.lilboard.board.application.port.in.CreateBoardUseCase;
import com.lilamaris.lilboard.board.application.port.in.command.CreateBoardCommand;
import com.lilamaris.lilboard.board.application.port.in.result.CreateBoardResult;
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

class BoardControllerTest {
    private CreateBoardUseCase useCase;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        useCase = mock(CreateBoardUseCase.class);
        mvc = MockMvcBuilders.standaloneSetup(new BoardController(useCase)).build();
    }

    @Test
    @DisplayName("유효한 생성 요청이면 유스케이스 결과와 201을 반환한다")
    void createsBoard() throws Exception {
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");
        Instant time = Instant.parse("2026-01-01T00:00:00Z");
        when(useCase.create(new CreateBoardCommand("게시판", "설명")))
                .thenReturn(new CreateBoardResult(id, "게시판", "설명", time));
        mvc.perform(
                        post("/api/boards")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"게시판\",\"description\":\"설명\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("게시판"))
                .andExpect(jsonPath("$.description").value("설명"))
                .andExpect(jsonPath("$.createdAt").value(time.toString()));
    }

    @Test
    @DisplayName("이름 50자와 설명 300자의 경계값이면 생성에 성공한다")
    void acceptsMaximumLengths() throws Exception {
        String name = "a".repeat(50);
        String description = "b".repeat(300);
        when(useCase.create(new CreateBoardCommand(name, description)))
                .thenReturn(
                        new CreateBoardResult(
                                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                                name,
                                description,
                                Instant.parse("2026-01-01T00:00:00Z")));
        mvc.perform(
                        post("/api/boards")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"name\":\""
                                                + name
                                                + "\",\"description\":\""
                                                + description
                                                + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.description").value(description));
    }

    @ParameterizedTest
    @MethodSource("invalidBodies")
    @DisplayName("필수 값 누락·공백·길이 초과 또는 잘못된 JSON이면 400을 반환하고 생성하지 않는다")
    void rejectsInvalidBody(String body) throws Exception {
        mvc.perform(post("/api/boards").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(useCase);
    }

    static Stream<String> invalidBodies() {
        return Stream.of(
                "{}",
                "{\"name\":\" \",\"description\":\"설명\"}",
                "{\"name\":\"게시판\",\"description\":\" \"}",
                "{\"name\":null,\"description\":\"설명\"}",
                "{\"name\":\"게시판\",\"description\":null}",
                "{\"name\":\"" + "a".repeat(51) + "\",\"description\":\"설명\"}",
                "{\"name\":\"게시판\",\"description\":\"" + "b".repeat(301) + "\"}",
                "{",
                "");
    }
}
