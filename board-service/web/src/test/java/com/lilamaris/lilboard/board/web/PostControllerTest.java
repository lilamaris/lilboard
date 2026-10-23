package com.lilamaris.lilboard.board.web;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.lilamaris.lilboard.board.application.port.in.CreatePostUseCase;
import com.lilamaris.lilboard.board.application.port.in.FindPostUseCase;
import com.lilamaris.lilboard.board.application.port.in.FindPostsUseCase;
import com.lilamaris.lilboard.board.application.port.in.UpdatePostUseCase;
import com.lilamaris.lilboard.board.application.port.in.command.CreatePostCommand;
import com.lilamaris.lilboard.board.application.port.in.command.UpdatePostCommand;
import com.lilamaris.lilboard.board.application.port.in.query.FindPostQuery;
import com.lilamaris.lilboard.board.application.port.in.query.FindPostsQuery;
import com.lilamaris.lilboard.board.application.port.in.result.CreatePostResult;
import com.lilamaris.lilboard.board.application.port.in.result.FindPostResult;
import com.lilamaris.lilboard.board.application.port.in.result.UpdatePostResult;
import com.lilamaris.lilboard.kernel.pagination.OffsetPage;
import com.lilamaris.lilboard.kernel.pagination.OffsetPageRequest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PostControllerTest {
    private CreatePostUseCase useCase;
    private UpdatePostUseCase updateUseCase;
    private FindPostUseCase findUseCase;
    private FindPostsUseCase listUseCase;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        useCase = mock(CreatePostUseCase.class);
        updateUseCase = mock(UpdatePostUseCase.class);
        findUseCase = mock(FindPostUseCase.class);
        listUseCase = mock(FindPostsUseCase.class);
        mvc =
                MockMvcBuilders.standaloneSetup(
                                new PostController(
                                        useCase, updateUseCase, findUseCase, listUseCase))
                        .build();
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

    @Test
    @DisplayName("유효한 수정 요청이면 경로의 ID로 수정하고 결과와 200을 반환한다")
    void updatesPost() throws Exception {
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID boardId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        Instant updatedAt = Instant.parse("2026-01-02T00:00:00Z");
        when(updateUseCase.update(new UpdatePostCommand(id, "수정 제목", "첫 번째 줄\n두 번째 줄")))
                .thenReturn(
                        new UpdatePostResult(
                                id, boardId, "수정 제목", "첫 번째 줄\n두 번째 줄", createdAt, updatedAt));

        mvc.perform(
                        put("/api/posts/{postId}", id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"title\":\"수정 제목\",\"content\":\"첫 번째 줄\\n두 번째 줄\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.boardId").value(boardId.toString()))
                .andExpect(jsonPath("$.title").value("수정 제목"))
                .andExpect(jsonPath("$.content").value("첫 번째 줄\n두 번째 줄"))
                .andExpect(jsonPath("$.createdAt").value(createdAt.toString()))
                .andExpect(jsonPath("$.updatedAt").value(updatedAt.toString()));
    }

    @Test
    @DisplayName("제목 300자와 본문 50000자의 경계값이면 수정에 성공한다")
    void acceptsMaximumUpdateLengths() throws Exception {
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID boardId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        String title = "a".repeat(300);
        String content = "b".repeat(50000);
        when(updateUseCase.update(new UpdatePostCommand(id, title, content)))
                .thenReturn(
                        new UpdatePostResult(
                                id,
                                boardId,
                                title,
                                content,
                                Instant.parse("2026-01-01T00:00:00Z"),
                                Instant.parse("2026-01-02T00:00:00Z")));

        mvc.perform(
                        put("/api/posts/{postId}", id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"title\":\""
                                                + title
                                                + "\",\"content\":\""
                                                + content
                                                + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(title))
                .andExpect(jsonPath("$.content").value(content));
    }

    @ParameterizedTest
    @MethodSource("invalidUpdateBodies")
    @DisplayName("수정 요청의 필수 값 누락·공백·길이 초과 또는 잘못된 JSON이면 400을 반환하고 수정하지 않는다")
    void rejectsInvalidUpdateBody(String body) throws Exception {
        mvc.perform(
                        put("/api/posts/00000000-0000-0000-0000-000000000001")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(updateUseCase);
    }

    @Test
    @DisplayName("게시글 ID가 UUID 형식이 아니면 400을 반환하고 수정하지 않는다")
    void rejectsInvalidPostId() throws Exception {
        mvc.perform(
                        put("/api/posts/invalid-id")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"title\":\"제목\",\"content\":\"본문\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(updateUseCase);
    }

    @Test
    @DisplayName("유효한 단건 조회 요청이면 경로의 ID로 조회하고 게시글 정보와 200을 반환한다")
    void findsPost() throws Exception {
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID boardId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        Instant updatedAt = Instant.parse("2026-01-02T00:00:00Z");
        when(findUseCase.find(new FindPostQuery(id)))
                .thenReturn(
                        new FindPostResult(
                                id, boardId, "수정 제목", "첫 번째 줄\n두 번째 줄", createdAt, updatedAt));

        mvc.perform(get("/api/posts/{postId}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.boardId").value(boardId.toString()))
                .andExpect(jsonPath("$.title").value("수정 제목"))
                .andExpect(jsonPath("$.content").value("첫 번째 줄\n두 번째 줄"))
                .andExpect(jsonPath("$.createdAt").value(createdAt.toString()))
                .andExpect(jsonPath("$.updatedAt").value(updatedAt.toString()));
    }

    @Test
    @DisplayName("수정하지 않은 게시글을 조회하면 수정 시각이 비어 있는 결과와 200을 반환한다")
    void findsPostWithoutUpdatedAt() throws Exception {
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID boardId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        when(findUseCase.find(new FindPostQuery(id)))
                .thenReturn(new FindPostResult(id, boardId, "제목", "본문", createdAt, null));

        mvc.perform(get("/api/posts/{postId}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.updatedAt").isEmpty());
    }

    @Test
    @DisplayName("단건 조회의 게시글 ID가 UUID 형식이 아니면 400을 반환하고 조회하지 않는다")
    void rejectsInvalidFindPostId() throws Exception {
        mvc.perform(get("/api/posts/invalid-id")).andExpect(status().isBadRequest());
        verifyNoInteractions(findUseCase, listUseCase);
    }

    @Test
    @DisplayName("단건 조회할 게시글이 존재하지 않으면 404를 반환한다")
    void returnsNotFoundWhenPostIsMissing() throws Exception {
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");
        when(findUseCase.find(new FindPostQuery(id)))
                .thenThrow(new IllegalArgumentException("Post를 찾을 수 없습니다."));

        mvc.perform(get("/api/posts/{postId}", id)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("목록 조회 조건을 생략하면 첫 페이지 20개로 조회하고 빈 결과와 페이지 정보를 반환한다")
    void findsEmptyPageWithDefaultParameters() throws Exception {
        var query = new FindPostsQuery(OffsetPageRequest.of(0, 20));
        when(listUseCase.find(query)).thenReturn(OffsetPage.of(List.of(), 0, 20, 0));

        mvc.perform(get("/api/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("페이지 번호와 크기를 지정하면 게시글 순서와 전체 개수를 유지한 목록을 반환한다")
    void findsPostPage() throws Exception {
        UUID firstId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID secondId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID boardId = UUID.fromString("00000000-0000-0000-0000-000000000003");
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        Instant updatedAt = Instant.parse("2026-01-02T00:00:00Z");
        var query = new FindPostsQuery(OffsetPageRequest.of(1, 2));
        when(listUseCase.find(query))
                .thenReturn(
                        OffsetPage.of(
                                List.of(
                                        new FindPostResult(
                                                firstId, boardId, "제목 A", "본문 A", createdAt,
                                                updatedAt),
                                        new FindPostResult(
                                                secondId, boardId, "제목 B", "본문 B", createdAt,
                                                null)),
                                1,
                                2,
                                5));

        mvc.perform(get("/api/posts").param("page", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].id").value(firstId.toString()))
                .andExpect(jsonPath("$.items[0].boardId").value(boardId.toString()))
                .andExpect(jsonPath("$.items[0].title").value("제목 A"))
                .andExpect(jsonPath("$.items[0].content").value("본문 A"))
                .andExpect(jsonPath("$.items[0].createdAt").value(createdAt.toString()))
                .andExpect(jsonPath("$.items[0].updatedAt").value(updatedAt.toString()))
                .andExpect(jsonPath("$.items[1].id").value(secondId.toString()))
                .andExpect(jsonPath("$.items[1].title").value("제목 B"))
                .andExpect(jsonPath("$.items[1].updatedAt").isEmpty())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(5));
    }

    @Test
    @DisplayName("마지막 페이지를 벗어나면 빈 목록과 실제 전체 개수를 반환한다")
    void findsPageBeyondLast() throws Exception {
        var query = new FindPostsQuery(OffsetPageRequest.of(3, 20));
        when(listUseCase.find(query)).thenReturn(OffsetPage.of(List.of(), 3, 20, 5));

        mvc.perform(get("/api/posts").param("page", "3").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.page").value(3))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(5));
    }

    @Test
    @DisplayName("offset이 Integer.MAX_VALUE이면 목록 조회를 허용한다")
    void acceptsMaximumOffset() throws Exception {
        var query = new FindPostsQuery(OffsetPageRequest.of(Integer.MAX_VALUE, 1));
        when(listUseCase.find(query)).thenReturn(OffsetPage.of(List.of(), Integer.MAX_VALUE, 1, 0));

        mvc.perform(get("/api/posts").param("page", "2147483647").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.page").value(Integer.MAX_VALUE))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @ParameterizedTest
    @CsvSource({
        "-1, 20",
        "0, 0",
        "0, -1",
        "invalid, 20",
        "0, invalid",
        "1.5, 20",
        "0, 1.5",
        "2147483648, 1",
        "0, 2147483648",
        "1073741824, 2",
        "2147483647, 2147483647"
    })
    @DisplayName("페이지 번호·크기가 유효하지 않거나 offset 범위를 초과하면 400을 반환하고 조회하지 않는다")
    void rejectsInvalidPageParameters(String page, String size) throws Exception {
        mvc.perform(get("/api/posts").param("page", page).param("size", size))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(findUseCase, listUseCase);
    }

    static Stream<String> invalidUpdateBodies() {
        return Stream.of(
                "{}",
                "{\"title\":\"제목\"}",
                "{\"content\":\"본문\"}",
                "{\"title\":\"\",\"content\":\"본문\"}",
                "{\"title\":\"제목\",\"content\":\"\"}",
                "{\"title\":\" \",\"content\":\"본문\"}",
                "{\"title\":\"제목\",\"content\":\" \"}",
                "{\"title\":null,\"content\":\"본문\"}",
                "{\"title\":\"제목\",\"content\":null}",
                "{\"title\":\"" + "a".repeat(301) + "\",\"content\":\"본문\"}",
                "{\"title\":\"제목\",\"content\":\"" + "b".repeat(50001) + "\"}",
                "{",
                "");
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
