package com.lilamaris.lilboard.board.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.lilamaris.lilboard.board.application.port.in.command.UpdateCommentCommand;
import com.lilamaris.lilboard.board.application.port.in.result.UpdateCommentResult;
import com.lilamaris.lilboard.board.application.port.out.CommentFindPort;
import com.lilamaris.lilboard.board.application.port.out.CommentSavePort;
import com.lilamaris.lilboard.board.domain.Comment;
import com.lilamaris.lilboard.kernel.exception.IllegalComparisonConditionException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateCommentService 테스트")
class UpdateCommentServiceTest {
    private static final UUID COMMENT_ID = UUID.fromString("c9bf9a5c-7a33-4b0e-9c8a-1f2e3d4c5b6a");
    private static final UUID POST_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final Instant CREATED_AT = Instant.parse("2024-01-15T10:30:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2024-01-15T11:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(UPDATED_AT, ZoneId.of("UTC"));

    @Mock private CommentSavePort commentSavePort;

    @Mock private CommentFindPort commentFindPort;

    private UpdateCommentService service() {
        return new UpdateCommentService(commentSavePort, commentFindPort, FIXED_CLOCK);
    }

    @DisplayName("정상: 명령이 주어지면 찾은 댓글을 클럭 시각으로 수정하고 저장 포트에 전달한다")
    @Test
    void updatesFoundCommentWithClockTimeAndSavesIt() {
        // given
        Comment comment = Comment.of(COMMENT_ID, POST_ID, "기존 본문", CREATED_AT);
        when(commentFindPort.findVisibleById(COMMENT_ID)).thenReturn(Optional.of(comment));
        when(commentSavePort.save(any(Comment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        UpdateCommentResult result =
                service().update(new UpdateCommentCommand(COMMENT_ID, POST_ID, "수정된 본문"));

        // then
        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        verify(commentSavePort).save(captor.capture());

        Comment saved = captor.getValue();
        assertThat(saved.getId()).isEqualTo(COMMENT_ID);
        assertThat(saved.getPostId()).isEqualTo(POST_ID);
        assertThat(saved.getContent()).isEqualTo("수정된 본문");
        assertThat(saved.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(saved.getUpdatedAt()).isEqualTo(UPDATED_AT);

        assertThat(result.id()).isEqualTo(COMMENT_ID);
        assertThat(result.content()).isEqualTo("수정된 본문");
        assertThat(result.updatedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    @DisplayName("클럭 시각이 생성 시각보다 빠르면 예외를 전파하고 수정 상태를 변경하거나 저장하지 않는다")
    void rejectsUpdateBeforeCreationWithoutSaving() {
        Comment comment = Comment.of(COMMENT_ID, POST_ID, "기존 본문", CREATED_AT);
        when(commentFindPort.findVisibleById(COMMENT_ID)).thenReturn(Optional.of(comment));
        Clock earlierClock = Clock.fixed(CREATED_AT.minusNanos(1), ZoneId.of("UTC"));
        UpdateCommentService service =
                new UpdateCommentService(commentSavePort, commentFindPort, earlierClock);

        assertThatThrownBy(
                        () -> service.update(new UpdateCommentCommand(COMMENT_ID, POST_ID, "새 본문")))
                .isInstanceOf(IllegalComparisonConditionException.class);

        assertThat(comment.getContent()).isEqualTo("기존 본문");
        assertThat(comment.getUpdatedAt()).isNull();
        verifyNoInteractions(commentSavePort);
    }

    @DisplayName("해당 ID의 댓글이 없으면 IllegalArgumentException을 던지고 저장하지 않는다")
    @Test
    void throwsIllegalArgumentExceptionWhenCommentNotFound() {
        // given
        when(commentFindPort.findVisibleById(COMMENT_ID)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(
                        () ->
                                service()
                                        .update(
                                                new UpdateCommentCommand(
                                                        COMMENT_ID, POST_ID, "수정된 본문")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("댓글을 찾을 수 없습니다.");

        verifyNoInteractions(commentSavePort);
    }

    @DisplayName("실패: 수정 명령이 null이면 NullPointerException을 던지고 포트를 호출하지 않는다")
    @Test
    void throwsNullPointerExceptionWhenCommandIsNull() {
        // when & then
        assertThatThrownBy(() -> service().update(null)).isInstanceOf(NullPointerException.class);

        verifyNoInteractions(commentFindPort, commentSavePort);
    }

    @DisplayName("댓글 저장 포트에서 예외가 발생하면 호출자에게 전파한다")
    @Test
    void propagatesExceptionWhenSavePortFails() {
        // given
        Comment comment = Comment.of(COMMENT_ID, POST_ID, "기존 본문", CREATED_AT);
        when(commentFindPort.findVisibleById(COMMENT_ID)).thenReturn(Optional.of(comment));
        when(commentSavePort.save(any(Comment.class))).thenThrow(new RuntimeException("DB 장애"));

        // when & then
        assertThatThrownBy(
                        () ->
                                service()
                                        .update(
                                                new UpdateCommentCommand(
                                                        COMMENT_ID, POST_ID, "수정된 본문")))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DB 장애");
    }
}
