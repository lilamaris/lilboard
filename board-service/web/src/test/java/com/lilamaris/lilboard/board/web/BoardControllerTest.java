package com.lilamaris.lilboard.board.web;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.lilamaris.lilboard.board.application.port.in.CreateBoardUseCase;
import com.lilamaris.lilboard.board.application.port.in.FindBoardUseCase;
import com.lilamaris.lilboard.board.application.port.in.FindBoardsUseCase;
import com.lilamaris.lilboard.board.application.port.in.UpdateBoardUseCase;
import com.lilamaris.lilboard.board.application.port.in.command.CreateBoardCommand;
import com.lilamaris.lilboard.board.application.port.in.command.UpdateBoardCommand;
import com.lilamaris.lilboard.board.application.port.in.query.FindBoardQuery;
import com.lilamaris.lilboard.board.application.port.in.query.FindBoardsQuery;
import com.lilamaris.lilboard.board.application.port.in.result.CreateBoardResult;
import com.lilamaris.lilboard.board.application.port.in.result.FindBoardResult;
import com.lilamaris.lilboard.board.application.port.in.result.UpdateBoardResult;
import com.lilamaris.lilboard.kernel.pagination.OffsetPage;
import java.time.Instant;
import java.util.List;
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
    private UpdateBoardUseCase updateUseCase;
    private FindBoardUseCase findBoardUseCase;
    private FindBoardsUseCase findBoardsUseCase;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        useCase = mock(CreateBoardUseCase.class);
        updateUseCase = mock(UpdateBoardUseCase.class);
        findBoardUseCase = mock(FindBoardUseCase.class);
        findBoardsUseCase = mock(FindBoardsUseCase.class);
        mvc =
                MockMvcBuilders.standaloneSetup(
                                new BoardController(
                                        useCase,
                                        updateUseCase,
                                        findBoardUseCase,
                                        findBoardsUseCase))
                        .build();
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

    @Test
    @DisplayName("유효한 수정 요청이면 경로의 ID로 수정하고 결과와 200을 반환한다")
    void updatesBoard() throws Exception {
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        Instant updatedAt = Instant.parse("2026-01-02T00:00:00Z");
        when(updateUseCase.update(new UpdateBoardCommand(id, "수정 게시판", "수정 설명")))
                .thenReturn(new UpdateBoardResult(id, "수정 게시판", "수정 설명", createdAt, updatedAt));

        mvc.perform(
                        put("/api/boards/{boardId}", id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"수정 게시판\",\"description\":\"수정 설명\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("수정 게시판"))
                .andExpect(jsonPath("$.description").value("수정 설명"))
                .andExpect(jsonPath("$.createdAt").value(createdAt.toString()))
                .andExpect(jsonPath("$.updatedAt").value(updatedAt.toString()));
    }

    @Test
    @DisplayName("이름 50자와 설명 300자의 경계값이면 수정에 성공한다")
    void acceptsMaximumUpdateLengths() throws Exception {
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");
        String name = "a".repeat(50);
        String description = "b".repeat(300);
        when(updateUseCase.update(new UpdateBoardCommand(id, name, description)))
                .thenReturn(
                        new UpdateBoardResult(
                                id,
                                name,
                                description,
                                Instant.parse("2026-01-01T00:00:00Z"),
                                Instant.parse("2026-01-02T00:00:00Z")));

        mvc.perform(
                        put("/api/boards/{boardId}", id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"name\":\""
                                                + name
                                                + "\",\"description\":\""
                                                + description
                                                + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.description").value(description));
    }

    @ParameterizedTest
    @MethodSource("invalidBodies")
    @DisplayName("수정 요청의 필수 값 누락·공백·길이 초과 또는 잘못된 JSON이면 400을 반환한다")
    void rejectsInvalidUpdateBody(String body) throws Exception {
        mvc.perform(
                        put("/api/boards/00000000-0000-0000-0000-000000000001")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(updateUseCase);
    }

    @Test
    @DisplayName("게시판 ID가 UUID 형식이 아니면 400을 반환하고 수정하지 않는다")
    void rejectsInvalidBoardId() throws Exception {
        mvc.perform(
                        put("/api/boards/invalid-id")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"게시판\",\"description\":\"설명\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(updateUseCase);
    }

    @Test
    @DisplayName("유효한 게시판 ID로 단건 조회하면 결과와 200을 반환한다")
    void findBoard() throws Exception {
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        Instant updatedAt = Instant.parse("2026-01-02T00:00:00Z");
        when(findBoardUseCase.find(new FindBoardQuery(id)))
                .thenReturn(new FindBoardResult(id, "게시판", "설명", createdAt, updatedAt));

        mvc.perform(get("/api/boards/{boardId}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("게시판"))
                .andExpect(jsonPath("$.description").value("설명"))
                .andExpect(jsonPath("$.createdAt").value(createdAt.toString()))
                .andExpect(jsonPath("$.updatedAt").value(updatedAt.toString()));
    }

    @Test
    @DisplayName("존재하지 않는 게시판 ID로 조회하면 404를 반환한다")
    void findBoardNotFound() throws Exception {
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");
        when(findBoardUseCase.find(new FindBoardQuery(id)))
                .thenThrow(new IllegalArgumentException("Board를 찾을 수 없습니다."));

        mvc.perform(get("/api/boards/{boardId}", id)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("게시판 ID가 UUID 형식이 아니면 400을 반환하고 조회하지 않는다")
    void findBoardInvalidId() throws Exception {
        mvc.perform(get("/api/boards/invalid-id")).andExpect(status().isBadRequest());
        verifyNoInteractions(findBoardUseCase);
    }

    @Test
    @DisplayName("유효한 페이지 요청으로 목록 조회하면 결과와 200을 반환한다")
    void findBoards() throws Exception {
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        FindBoardResult result = new FindBoardResult(id, "게시판", "설명", createdAt, null);
        OffsetPage<FindBoardResult> page = new OffsetPage<>(List.of(result), 0, 20, 1);
        when(findBoardsUseCase.find(any(FindBoardsQuery.class))).thenReturn(page);

        mvc.perform(get("/api/boards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(id.toString()))
                .andExpect(jsonPath("$.items[0].name").value("게시판"))
                .andExpect(jsonPath("$.items[0].description").value("설명"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("음수 페이지 번호로 목록 조회하면 400을 반환한다")
    void findBoardsInvalidPage() throws Exception {
        mvc.perform(get("/api/boards").param("page", "-1")).andExpect(status().isBadRequest());
        verifyNoInteractions(findBoardsUseCase);
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
