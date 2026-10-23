package com.lilamaris.lilboard.board.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.lilamaris.lilboard.board.application.port.in.UpdatePostUseCase;
import com.lilamaris.lilboard.board.application.port.in.command.UpdatePostCommand;
import com.lilamaris.lilboard.board.application.port.out.PostFindPort;
import com.lilamaris.lilboard.board.application.port.out.PostSavePort;
import com.lilamaris.lilboard.board.domain.Post;
import jakarta.validation.ConstraintViolationException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

class UpdatePostUseCaseValidationTest {
    private static final UUID POST_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID BOARD_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant UPDATED_AT = CREATED_AT.plusSeconds(1);

    private AnnotationConfigApplicationContext context;
    private PostFindPort findPort;
    private PostSavePort savePort;
    private UpdatePostUseCase useCase;

    @BeforeEach
    void setUp() {
        findPort = mock(PostFindPort.class);
        savePort = mock(PostSavePort.class);
        Clock clock = Clock.fixed(UPDATED_AT, ZoneOffset.UTC);
        context = new AnnotationConfigApplicationContext();
        context.registerBean(MethodValidationPostProcessor.class);
        context.registerBean(
                UpdatePostService.class, () -> new UpdatePostService(savePort, findPort, clock));
        context.refresh();
        useCase = context.getBean(UpdatePostUseCase.class);
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    @DisplayName("수정 명령이 null이면 검증 예외를 던지고 게시글을 조회하거나 저장하지 않는다")
    void rejectsNullCommand() {
        assertThatThrownBy(() -> useCase.update(null))
                .isInstanceOf(ConstraintViolationException.class);
        verifyNoInteractions(findPort, savePort);
    }

    @Test
    @DisplayName("수정할 게시글 ID가 null이면 검증 예외를 던지고 게시글을 조회하거나 저장하지 않는다")
    void rejectsNullPostId() {
        assertThatThrownBy(() -> useCase.update(new UpdatePostCommand(null, "제목", "본문")))
                .isInstanceOf(ConstraintViolationException.class);
        verifyNoInteractions(findPort, savePort);
    }

    @ParameterizedTest
    @MethodSource("invalidTitles")
    @DisplayName("수정 제목이 누락·공백·길이 초과이면 검증 예외를 던지고 게시글을 조회하거나 저장하지 않는다")
    void rejectsInvalidTitle(String title) {
        assertThatThrownBy(() -> useCase.update(new UpdatePostCommand(POST_ID, title, "본문")))
                .isInstanceOf(ConstraintViolationException.class);
        verifyNoInteractions(findPort, savePort);
    }

    @ParameterizedTest
    @MethodSource("invalidContents")
    @DisplayName("수정 본문이 누락·공백·길이 초과이면 검증 예외를 던지고 게시글을 조회하거나 저장하지 않는다")
    void rejectsInvalidContent(String content) {
        assertThatThrownBy(() -> useCase.update(new UpdatePostCommand(POST_ID, "제목", content)))
                .isInstanceOf(ConstraintViolationException.class);
        verifyNoInteractions(findPort, savePort);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 300})
    @DisplayName("수정 제목이 최소·최대 길이이면 검증을 통과하고 수정 결과를 반환한다")
    void acceptsTitleAtLengthBoundaries(int length) {
        stubExistingPost();
        String title = "가".repeat(length);

        var result = useCase.update(new UpdatePostCommand(POST_ID, title, "새 본문"));

        assertThat(result.title()).isEqualTo(title);
        assertThat(result.updatedAt()).isEqualTo(UPDATED_AT);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 50000})
    @DisplayName("수정 본문이 최소·최대 길이이면 검증을 통과하고 수정 결과를 반환한다")
    void acceptsContentAtLengthBoundaries(int length) {
        stubExistingPost();
        String content = "나".repeat(length);

        var result = useCase.update(new UpdatePostCommand(POST_ID, "새 제목", content));

        assertThat(result.content()).isEqualTo(content);
        assertThat(result.updatedAt()).isEqualTo(UPDATED_AT);
    }

    private void stubExistingPost() {
        Post post = Post.of(POST_ID, BOARD_ID, "기존 제목", "기존 본문", CREATED_AT);
        when(findPort.findById(POST_ID)).thenReturn(Optional.of(post));
        when(savePort.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    static Stream<String> invalidTitles() {
        return Stream.of(null, "", " ", "\t\n", "가".repeat(301));
    }

    static Stream<String> invalidContents() {
        return Stream.of(null, "", " ", "\t\n", "나".repeat(50001));
    }
}
