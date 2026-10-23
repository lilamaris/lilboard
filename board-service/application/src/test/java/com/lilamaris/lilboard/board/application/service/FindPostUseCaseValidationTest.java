package com.lilamaris.lilboard.board.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.lilamaris.lilboard.board.application.port.in.FindPostUseCase;
import com.lilamaris.lilboard.board.application.port.in.FindPostsUseCase;
import com.lilamaris.lilboard.board.application.port.in.query.FindPostQuery;
import com.lilamaris.lilboard.board.application.port.in.query.FindPostsQuery;
import com.lilamaris.lilboard.board.application.port.in.result.FindPostResult;
import com.lilamaris.lilboard.board.application.port.out.PostFindPort;
import com.lilamaris.lilboard.board.application.port.out.PostListPort;
import com.lilamaris.lilboard.board.domain.Post;
import com.lilamaris.lilboard.kernel.pagination.OffsetPage;
import com.lilamaris.lilboard.kernel.pagination.OffsetPageRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

class FindPostUseCaseValidationTest {
    private static final UUID POST_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID BOARD_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");

    private AnnotationConfigApplicationContext context;
    private PostFindPort findPort;
    private PostListPort listPort;

    @BeforeEach
    void setUp() {
        findPort = mock(PostFindPort.class);
        listPort = mock(PostListPort.class);
        context = new AnnotationConfigApplicationContext();
        context.registerBean(MethodValidationPostProcessor.class);
        context.registerBean(FindPostService.class, () -> new FindPostService(findPort));
        context.registerBean(FindPostsService.class, () -> new FindPostsService(listPort));
        context.refresh();
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    @DisplayName("단건 조회 조건이 null이면 검증 예외를 던지고 게시글을 조회하지 않는다")
    void rejectsNullFindQuery() {
        assertThatThrownBy(() -> context.getBean(FindPostUseCase.class).find(null))
                .isInstanceOf(ConstraintViolationException.class);
        verifyNoInteractions(findPort, listPort);
    }

    @Test
    @DisplayName("조회할 게시글 ID가 null이면 검증 예외를 던지고 게시글을 조회하지 않는다")
    void rejectsNullPostId() {
        assertThatThrownBy(
                        () -> context.getBean(FindPostUseCase.class).find(new FindPostQuery(null)))
                .isInstanceOf(ConstraintViolationException.class);
        verifyNoInteractions(findPort, listPort);
    }

    @Test
    @DisplayName("목록 조회 조건이 null이면 검증 예외를 던지고 목록을 조회하지 않는다")
    void rejectsNullListQuery() {
        assertThatThrownBy(() -> context.getBean(FindPostsUseCase.class).find(null))
                .isInstanceOf(ConstraintViolationException.class);
        verifyNoInteractions(findPort, listPort);
    }

    @Test
    @DisplayName("목록 조회의 페이지 요청이 null이면 검증 예외를 던지고 목록을 조회하지 않는다")
    void rejectsNullPageRequest() {
        assertThatThrownBy(
                        () ->
                                context.getBean(FindPostsUseCase.class)
                                        .find(new FindPostsQuery(null)))
                .isInstanceOf(ConstraintViolationException.class);
        verifyNoInteractions(findPort, listPort);
    }

    @Test
    @DisplayName("유효한 게시글 ID이면 메서드 검증을 통과하고 조회 결과를 반환한다")
    void acceptsValidFindQuery() {
        Post post = Post.of(POST_ID, BOARD_ID, "제목", "본문", CREATED_AT);
        when(findPort.findById(POST_ID)).thenReturn(Optional.of(post));

        var result = context.getBean(FindPostUseCase.class).find(new FindPostQuery(POST_ID));

        assertThat(result)
                .isEqualTo(new FindPostResult(POST_ID, BOARD_ID, "제목", "본문", CREATED_AT, null));
    }

    @Test
    @DisplayName("유효한 목록 조회 조건이면 메서드 검증을 통과하고 조회 결과를 반환한다")
    void acceptsValidListQuery() {
        var request = OffsetPageRequest.of(0, 20);
        Post post = Post.of(POST_ID, BOARD_ID, "제목", "본문", CREATED_AT);
        when(listPort.findAll(request)).thenReturn(OffsetPage.of(List.of(post), 0, 20, 1));

        var result = context.getBean(FindPostsUseCase.class).find(new FindPostsQuery(request));

        assertThat(result)
                .isEqualTo(
                        OffsetPage.of(
                                List.of(
                                        new FindPostResult(
                                                POST_ID, BOARD_ID, "제목", "본문", CREATED_AT, null)),
                                0,
                                20,
                                1));
    }
}
