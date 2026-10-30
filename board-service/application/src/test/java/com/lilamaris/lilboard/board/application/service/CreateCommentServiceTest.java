package com.lilamaris.lilboard.board.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.lilamaris.lilboard.board.application.port.in.command.CreateCommentCommand;
import com.lilamaris.lilboard.board.application.port.in.result.CreateCommentResult;
import com.lilamaris.lilboard.board.application.port.out.CommentSavePort;
import com.lilamaris.lilboard.board.application.port.out.PostFindPort;
import com.lilamaris.lilboard.board.application.port.out.UuidGenerator;
import com.lilamaris.lilboard.board.domain.Comment;
import com.lilamaris.lilboard.board.domain.Post;
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
class CreateCommentServiceTest {
    private static final UUID COMMENT_ID = UUID.fromString("c9bf9a5c-7a33-4b0e-9c8a-1f2e3d4c5b6a");
    private static final UUID POST_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID BOARD_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final Instant CREATED_AT = Instant.parse("2024-01-15T10:30:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(CREATED_AT, ZoneId.of("UTC"));

    @Mock private CommentSavePort commentSavePort;

    @Mock private PostFindPort postFindPort;

    @Mock private UuidGenerator uuidGenerator;

    private CreateCommentService service() {
        return new CreateCommentService(commentSavePort, postFindPort, uuidGenerator, FIXED_CLOCK);
    }

    @DisplayName("정상: 명령이 주어지면 저장 포트가 반환한 댓글의 값으로 결과를 만든다")
    @Test
    void returnsResultBuiltFromSavedComment() {
        // given
        CreateCommentCommand command = new CreateCommentCommand(POST_ID, "첫 번째 댓글입니다");

        when(postFindPort.findVisibleById(POST_ID))
                .thenReturn(Optional.of(Post.of(POST_ID, BOARD_ID, "제목", "내용", CREATED_AT)));
        when(uuidGenerator.generate()).thenReturn(COMMENT_ID);
        when(commentSavePort.save(any(Comment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        CreateCommentResult result = service().create(command);

        // then
        assertThat(result.id()).isEqualTo(COMMENT_ID);
        assertThat(result.postId()).isEqualTo(POST_ID);
        assertThat(result.content()).isEqualTo("첫 번째 댓글입니다");
        assertThat(result.createdAt()).isEqualTo(CREATED_AT);
    }

    @DisplayName("정상: 게시글이 존재하면 제어 가능한 UUID 발급 포트와 시계로 만든 댓글을 저장 포트에 전달한다")
    @Test
    void savesCommentBuiltFromGeneratedIdAndFixedClock() {
        // given
        CreateCommentCommand command = new CreateCommentCommand(POST_ID, "첫 번째 댓글입니다");

        when(postFindPort.findVisibleById(POST_ID))
                .thenReturn(Optional.of(Post.of(POST_ID, BOARD_ID, "제목", "내용", CREATED_AT)));
        when(uuidGenerator.generate()).thenReturn(COMMENT_ID);
        when(commentSavePort.save(any(Comment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        service().create(command);

        // then
        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        verify(commentSavePort).save(captor.capture());

        Comment saved = captor.getValue();
        assertThat(saved.getId()).isEqualTo(COMMENT_ID);
        assertThat(saved.getPostId()).isEqualTo(POST_ID);
        assertThat(saved.getContent()).isEqualTo("첫 번째 댓글입니다");
        assertThat(saved.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(saved.getUpdatedAt()).isNull();
        assertThat(saved.getDeletedAt()).isNull();
    }

    @DisplayName("실패: 댓글 생성 명령이 null이면 NullPointerException을 던진다")
    @Test
    void throwsNullPointerExceptionWhenCommandIsNull() {
        // when & then
        assertThatThrownBy(() -> service().create(null)).isInstanceOf(NullPointerException.class);
    }

    @DisplayName("실패: 게시글이 없으면 IllegalArgumentException을 던지고 저장하지 않는다")
    @Test
    void throwsIllegalArgumentExceptionWhenPostNotFound() {
        // given
        CreateCommentCommand command = new CreateCommentCommand(POST_ID, "본문");

        when(postFindPort.findVisibleById(POST_ID)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service().create(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Post를 찾을 수 없습니다.");

        verifyNoInteractions(commentSavePort);
    }

    @DisplayName("실패: 댓글 저장 포트에서 예외가 발생하면 호출자에게 전파한다")
    @Test
    void propagatesExceptionWhenSavePortFails() {
        // given
        CreateCommentCommand command = new CreateCommentCommand(POST_ID, "본문");

        when(postFindPort.findVisibleById(POST_ID))
                .thenReturn(Optional.of(Post.of(POST_ID, BOARD_ID, "제목", "내용", CREATED_AT)));
        when(uuidGenerator.generate()).thenReturn(COMMENT_ID);
        when(commentSavePort.save(any(Comment.class))).thenThrow(new RuntimeException("DB 장애"));

        // when & then
        assertThatThrownBy(() -> service().create(command))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DB 장애");
    }
}
