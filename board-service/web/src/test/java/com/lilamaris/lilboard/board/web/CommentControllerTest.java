package com.lilamaris.lilboard.board.web;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.lilamaris.lilboard.board.application.port.in.CreateCommentUseCase;
import com.lilamaris.lilboard.board.application.port.in.DeleteCommentUseCase;
import com.lilamaris.lilboard.board.application.port.in.FindCommentUseCase;
import com.lilamaris.lilboard.board.application.port.in.FindCommentsUseCase;
import com.lilamaris.lilboard.board.application.port.in.UpdateCommentUseCase;
import com.lilamaris.lilboard.board.application.port.in.command.CreateCommentCommand;
import com.lilamaris.lilboard.board.application.port.in.command.DeleteCommentCommand;
import com.lilamaris.lilboard.board.application.port.in.command.UpdateCommentCommand;
import com.lilamaris.lilboard.board.application.port.in.query.FindCommentQuery;
import com.lilamaris.lilboard.board.application.port.in.query.FindCommentsQuery;
import com.lilamaris.lilboard.board.application.port.in.result.CreateCommentResult;
import com.lilamaris.lilboard.board.application.port.in.result.FindCommentResult;
import com.lilamaris.lilboard.board.application.port.in.result.UpdateCommentResult;
import com.lilamaris.lilboard.kernel.pagination.Cursor;
import com.lilamaris.lilboard.kernel.pagination.CursorPage;
import com.lilamaris.lilboard.kernel.pagination.CursorPageRequest;
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

class CommentControllerTest {
    private static final UUID POST_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID COMMENT_ID = UUID.fromString("c9bf9a5c-7a33-4b0e-9c8a-1f2e3d4c5b6a");
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-01-02T00:00:00Z");

    private CreateCommentUseCase createUseCase;
    private UpdateCommentUseCase updateUseCase;
    private DeleteCommentUseCase deleteUseCase;
    private FindCommentUseCase findUseCase;
    private FindCommentsUseCase findCommentsUseCase;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        createUseCase = mock(CreateCommentUseCase.class);
        updateUseCase = mock(UpdateCommentUseCase.class);
        deleteUseCase = mock(DeleteCommentUseCase.class);
        findUseCase = mock(FindCommentUseCase.class);
        findCommentsUseCase = mock(FindCommentsUseCase.class);
        mvc =
                MockMvcBuilders.standaloneSetup(
                                new CommentController(
                                        createUseCase,
                                        updateUseCase,
                                        deleteUseCase,
                                        findUseCase,
                                        findCommentsUseCase))
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .build();
    }

    private String commentsUrl() {
        return "/api/posts/" + POST_ID + "/comments";
    }

    private String commentUrl() {
        return "/api/posts/" + POST_ID + "/comments/" + COMMENT_ID;
    }

    @Test
    @DisplayName("유효한 생성 요청이면 유스케이스 결과와 201을 반환한다")
    void createsComment() throws Exception {
        when(createUseCase.create(new CreateCommentCommand(POST_ID, "본문")))
                .thenReturn(new CreateCommentResult(COMMENT_ID, POST_ID, "본문", CREATED_AT, null));

        mvc.perform(
                        post(commentsUrl())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"content\":\"본문\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(COMMENT_ID.toString()))
                .andExpect(jsonPath("$.postId").value(POST_ID.toString()))
                .andExpect(jsonPath("$.content").value("본문"))
                .andExpect(jsonPath("$.createdAt").value(CREATED_AT.toString()));
    }

    @Test
    @DisplayName("본문 5000자의 경계값이면 생성에 성공한다")
    void acceptsMaximumLength() throws Exception {
        String content = "a".repeat(5000);
        when(createUseCase.create(new CreateCommentCommand(POST_ID, content)))
                .thenReturn(
                        new CreateCommentResult(COMMENT_ID, POST_ID, content, CREATED_AT, null));

        mvc.perform(
                        post(commentsUrl())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"content\":\"" + content + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value(content));
    }

    @ParameterizedTest
    @MethodSource("invalidBodies")
    @DisplayName("필수 값 누락·공백·길이 초과 또는 잘못된 JSON이면 400을 반환하고 생성하지 않는다")
    void rejectsInvalidBody(String body) throws Exception {
        mvc.perform(post(commentsUrl()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("잘못된 요청"));
        verifyNoInteractions(createUseCase);
    }

    @Test
    @DisplayName("게시글이 조회되지 않으면 생성 요청에 404를 반환한다")
    void returnsNotFoundWhenPostIsNotVisibleForCreate() throws Exception {
        when(createUseCase.create(new CreateCommentCommand(POST_ID, "본문")))
                .thenThrow(new IllegalArgumentException("Post를 찾을 수 없습니다."));

        mvc.perform(
                        post(commentsUrl())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"content\":\"본문\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("유효한 수정 요청이면 경로의 ID로 수정하고 결과와 200을 반환한다")
    void updatesComment() throws Exception {
        when(updateUseCase.update(new UpdateCommentCommand(COMMENT_ID, POST_ID, "수정된 본문")))
                .thenReturn(
                        new UpdateCommentResult(
                                COMMENT_ID, POST_ID, "수정된 본문", CREATED_AT, UPDATED_AT));

        mvc.perform(
                        put(commentUrl())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"content\":\"수정된 본문\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(COMMENT_ID.toString()))
                .andExpect(jsonPath("$.postId").value(POST_ID.toString()))
                .andExpect(jsonPath("$.content").value("수정된 본문"))
                .andExpect(jsonPath("$.createdAt").value(CREATED_AT.toString()))
                .andExpect(jsonPath("$.updatedAt").value(UPDATED_AT.toString()));
    }

    @Test
    @DisplayName("수정할 댓글이 조회되지 않으면 404를 반환한다")
    void returnsNotFoundWhenCommentToUpdateIsNotVisible() throws Exception {
        when(updateUseCase.update(new UpdateCommentCommand(COMMENT_ID, POST_ID, "새 본문")))
                .thenThrow(new IllegalArgumentException("댓글을 찾을 수 없습니다."));

        mvc.perform(
                        put(commentUrl())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"content\":\"새 본문\"}"))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @MethodSource("invalidUpdateBodies")
    @DisplayName("수정 요청의 필수 값 누락·공백·길이 초과 또는 잘못된 JSON이면 400을 반환한다")
    void rejectsInvalidUpdateBody(String body) throws Exception {
        mvc.perform(put(commentUrl()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(updateUseCase);
    }

    @Test
    @DisplayName("댓글 ID가 UUID 형식이 아니면 400을 반환하고 수정하지 않는다")
    void rejectsInvalidCommentId() throws Exception {
        mvc.perform(
                        put("/api/posts/" + POST_ID + "/comments/invalid-id")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"content\":\"본문\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(updateUseCase);
    }

    @Test
    @DisplayName("유효한 게시글 ID와 댓글 ID로 삭제하면 204를 반환하고 삭제 유스케이스를 호출한다")
    void deletesComment() throws Exception {
        mvc.perform(delete(commentUrl())).andExpect(status().isNoContent());

        verify(deleteUseCase).delete(new DeleteCommentCommand(COMMENT_ID));
    }

    @Test
    @DisplayName("삭제할 댓글이 없으면 유스케이스가 IllegalArgumentException을 던지고 404를 반환한다")
    void deleteCommentNotFound() throws Exception {
        doThrow(new IllegalArgumentException("댓글을 찾을 수 없습니다."))
                .when(deleteUseCase)
                .delete(new DeleteCommentCommand(COMMENT_ID));

        mvc.perform(delete(commentUrl())).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("댓글 ID가 UUID 형식이 아니면 400을 반환하고 삭제하지 않는다")
    void rejectsInvalidCommentIdForDelete() throws Exception {
        mvc.perform(delete("/api/posts/" + POST_ID + "/comments/invalid-id"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(deleteUseCase);
    }

    @Test
    @DisplayName("유효한 댓글 ID로 단건 조회하면 결과와 200을 반환한다")
    void findComment() throws Exception {
        when(findUseCase.find(new FindCommentQuery(COMMENT_ID)))
                .thenReturn(
                        new FindCommentResult(COMMENT_ID, POST_ID, "본문", CREATED_AT, UPDATED_AT));

        mvc.perform(get(commentUrl()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(COMMENT_ID.toString()))
                .andExpect(jsonPath("$.postId").value(POST_ID.toString()))
                .andExpect(jsonPath("$.content").value("본문"))
                .andExpect(jsonPath("$.createdAt").value(CREATED_AT.toString()))
                .andExpect(jsonPath("$.updatedAt").value(UPDATED_AT.toString()));
    }

    @Test
    @DisplayName("존재하지 않는 댓글 ID로 조회하면 404를 반환한다")
    void findCommentNotFound() throws Exception {
        when(findUseCase.find(new FindCommentQuery(COMMENT_ID)))
                .thenThrow(new IllegalArgumentException("댓글을 찾을 수 없습니다."));

        mvc.perform(get(commentUrl()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("요청한 대상을 찾을 수 없습니다"))
                .andExpect(jsonPath("$.detail").value("댓글을 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("댓글 ID가 UUID 형식이 아니면 400을 반환하고 조회하지 않는다")
    void findCommentInvalidId() throws Exception {
        mvc.perform(get("/api/posts/" + POST_ID + "/comments/invalid-id"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(findUseCase);
    }

    @Test
    @DisplayName("커서 없이 첫 페이지를 조회하면 목록과 다음 커서를 반환한다")
    void findsFirstPageWithCursor() throws Exception {
        UUID firstId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID secondId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        FindCommentResult first = new FindCommentResult(firstId, POST_ID, "댓글 A", CREATED_AT, null);
        FindCommentResult second =
                new FindCommentResult(secondId, POST_ID, "댓글 B", CREATED_AT, null);
        Cursor<UUID> nextCursor = Cursor.of(CREATED_AT, secondId);
        CursorPage<FindCommentResult, UUID> page =
                CursorPage.of(List.of(first, second), nextCursor);

        var query = new FindCommentsQuery(POST_ID, new CursorPageRequest<>(null, 2));
        when(findCommentsUseCase.find(query)).thenReturn(page);

        mvc.perform(get(commentsUrl()).param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].id").value(firstId.toString()))
                .andExpect(jsonPath("$.items[0].content").value("댓글 A"))
                .andExpect(jsonPath("$.items[1].id").value(secondId.toString()))
                .andExpect(jsonPath("$.items[1].content").value("댓글 B"))
                .andExpect(jsonPath("$.nextCursor.createdAt").value(CREATED_AT.toString()))
                .andExpect(jsonPath("$.nextCursor.id").value(secondId.toString()));
    }

    @Test
    @DisplayName("다음 커서를 주면 해당 커서 이후의 댓글 페이지를 반환한다")
    void findsNextPageByCursor() throws Exception {
        UUID firstId = UUID.fromString("00000000-0000-0000-0000-000000000003");
        FindCommentResult first = new FindCommentResult(firstId, POST_ID, "댓글 C", CREATED_AT, null);
        CursorPage<FindCommentResult, UUID> page = CursorPage.of(List.of(first), null);

        Cursor<UUID> cursor =
                Cursor.of(CREATED_AT, UUID.fromString("00000000-0000-0000-0000-000000000002"));
        var query = new FindCommentsQuery(POST_ID, new CursorPageRequest<>(cursor, 2));
        when(findCommentsUseCase.find(query)).thenReturn(page);

        mvc.perform(
                        get(commentsUrl())
                                .param("cursorCreatedAt", CREATED_AT.toString())
                                .param("cursorId", "00000000-0000-0000-0000-000000000002")
                                .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(firstId.toString()))
                .andExpect(jsonPath("$.items[0].content").value("댓글 C"))
                .andExpect(jsonPath("$.nextCursor").isEmpty());
    }

    @Test
    @DisplayName("댓글이 없으면 빈 목록을 반환한다")
    void findsEmptyPage() throws Exception {
        var query = new FindCommentsQuery(POST_ID, new CursorPageRequest<>(null, 20));
        when(findCommentsUseCase.find(query)).thenReturn(CursorPage.of(List.of(), null));

        mvc.perform(get(commentsUrl()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.nextCursor").isEmpty());
    }

    static Stream<String> invalidBodies() {
        return Stream.of(
                "{}",
                "{\"content\":\" \"}",
                "{\"content\":null}",
                "{\"content\":\"" + "a".repeat(5001) + "\"}",
                "{",
                "");
    }

    static Stream<String> invalidUpdateBodies() {
        return Stream.of(
                "{}",
                "{\"content\":\" \"}",
                "{\"content\":null}",
                "{\"content\":\"" + "a".repeat(5001) + "\"}",
                "{",
                "");
    }
}
