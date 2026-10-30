package com.lilamaris.lilboard.board.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.lilamaris.lilboard.board.application.port.in.query.FindCommentQuery;
import com.lilamaris.lilboard.board.application.port.in.result.FindCommentResult;
import com.lilamaris.lilboard.board.application.port.out.CommentFindPort;
import com.lilamaris.lilboard.board.domain.Comment;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FindCommentServiceTest {
    private static final UUID COMMENT_ID = UUID.fromString("c9bf9a5c-7a33-4b0e-9c8a-1f2e3d4c5b6a");
    private static final UUID POST_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final Instant CREATED_AT = Instant.parse("2024-01-15T10:30:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2024-01-15T11:00:00Z");

    @Mock private CommentFindPort commentFindPort;

    @Test
    @DisplayName("해당 ID의 댓글이 있으면 댓글 정보를 반환한다")
    void returnsFoundComment() {
        // given
        Comment comment = Comment.of(COMMENT_ID, POST_ID, "본문", CREATED_AT);
        comment.updateContent("수정된 본문", UPDATED_AT);
        when(commentFindPort.findVisibleById(COMMENT_ID)).thenReturn(Optional.of(comment));

        FindCommentService service = new FindCommentService(commentFindPort);

        // when
        FindCommentResult result = service.find(new FindCommentQuery(COMMENT_ID));

        // then
        assertThat(result)
                .isEqualTo(
                        new FindCommentResult(
                                COMMENT_ID, POST_ID, "수정된 본문", CREATED_AT, UPDATED_AT));
    }

    @Test
    @DisplayName("해당 ID의 댓글이 없으면 IllegalArgumentException을 던진다")
    void throwsIllegalArgumentExceptionWhenCommentNotFound() {
        // given
        when(commentFindPort.findVisibleById(COMMENT_ID)).thenReturn(Optional.empty());
        FindCommentService service = new FindCommentService(commentFindPort);

        // when & then
        assertThatThrownBy(() -> service.find(new FindCommentQuery(COMMENT_ID)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("댓글을 찾을 수 없습니다.");
    }
}
