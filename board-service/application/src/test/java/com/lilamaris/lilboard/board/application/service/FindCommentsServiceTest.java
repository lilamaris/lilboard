package com.lilamaris.lilboard.board.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.lilamaris.lilboard.board.application.port.in.query.FindCommentsQuery;
import com.lilamaris.lilboard.board.application.port.in.result.FindCommentResult;
import com.lilamaris.lilboard.board.application.port.out.CommentListPort;
import com.lilamaris.lilboard.board.domain.Comment;
import com.lilamaris.lilboard.kernel.pagination.Cursor;
import com.lilamaris.lilboard.kernel.pagination.CursorPage;
import com.lilamaris.lilboard.kernel.pagination.CursorPageRequest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FindCommentsServiceTest {
    private static final UUID POST_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final Instant CREATED_AT = Instant.parse("2024-01-15T10:30:00Z");

    @Mock private CommentListPort commentListPort;

    @Test
    @DisplayName("목록 조회 시 항목 순서와 댓글 정보 및 커서 페이지 정보를 유지해 반환한다")
    void returnsCommentCursorPage() {
        // given
        UUID firstId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID secondId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        Comment first = Comment.of(firstId, POST_ID, "댓글 A", CREATED_AT);
        Comment second = Comment.of(secondId, POST_ID, "댓글 B", CREATED_AT);
        Cursor<UUID> nextCursor = Cursor.of(CREATED_AT, secondId);
        CursorPageRequest<UUID> request = CursorPageRequest.of(null, 2);
        when(commentListPort.findVisibleByPostId(POST_ID, request))
                .thenReturn(CursorPage.of(List.of(first, second), nextCursor));

        FindCommentsService service = new FindCommentsService(commentListPort);

        // when
        CursorPage<FindCommentResult, UUID> result =
                service.find(new FindCommentsQuery(POST_ID, request));

        // then
        assertThat(result.items())
                .containsExactly(
                        new FindCommentResult(firstId, POST_ID, "댓글 A", CREATED_AT, null),
                        new FindCommentResult(secondId, POST_ID, "댓글 B", CREATED_AT, null));
        assertThat(result.nextCursor()).isEqualTo(nextCursor);
        assertThat(result.hasNext()).isTrue();
    }

    @Test
    @DisplayName("다음 페이지 커서가 주어지면 해당 커서 이후의 댓글 페이지를 반환한다")
    void returnsNextPageByCursor() {
        // given
        UUID firstId = UUID.fromString("00000000-0000-0000-0000-000000000003");
        UUID secondId = UUID.fromString("00000000-0000-0000-0000-000000000004");
        Comment first = Comment.of(firstId, POST_ID, "댓글 C", CREATED_AT);
        Comment second = Comment.of(secondId, POST_ID, "댓글 D", CREATED_AT);
        Cursor<UUID> cursor =
                Cursor.of(CREATED_AT, UUID.fromString("00000000-0000-0000-0000-000000000002"));
        CursorPageRequest<UUID> request = CursorPageRequest.of(cursor, 2);
        when(commentListPort.findVisibleByPostId(POST_ID, request))
                .thenReturn(CursorPage.of(List.of(first, second), null));

        FindCommentsService service = new FindCommentsService(commentListPort);

        // when
        CursorPage<FindCommentResult, UUID> result =
                service.find(new FindCommentsQuery(POST_ID, request));

        // then
        assertThat(result.items())
                .containsExactly(
                        new FindCommentResult(firstId, POST_ID, "댓글 C", CREATED_AT, null),
                        new FindCommentResult(secondId, POST_ID, "댓글 D", CREATED_AT, null));
        assertThat(result.nextCursor()).isNull();
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    @DisplayName("댓글이 없으면 빈 페이지와 다음 커서 null을 반환한다")
    void returnsEmptyPage() {
        // given
        CursorPageRequest<UUID> request = CursorPageRequest.of(null, 20);
        when(commentListPort.findVisibleByPostId(POST_ID, request))
                .thenReturn(CursorPage.of(List.of(), null));
        FindCommentsService service = new FindCommentsService(commentListPort);

        // when
        CursorPage<FindCommentResult, UUID> result =
                service.find(new FindCommentsQuery(POST_ID, request));

        // then
        assertThat(result.items()).isEmpty();
        assertThat(result.nextCursor()).isNull();
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    @DisplayName("목록 조회 포트의 장애를 호출자에게 전파한다")
    void propagatesListFailure() {
        // given
        CursorPageRequest<UUID> request = CursorPageRequest.of(null, 20);
        var failure = new IllegalStateException("DB 장애");
        when(commentListPort.findVisibleByPostId(POST_ID, request)).thenThrow(failure);
        FindCommentsService service = new FindCommentsService(commentListPort);

        // when & then
        assertThatThrownBy(() -> service.find(new FindCommentsQuery(POST_ID, request)))
                .isSameAs(failure);
    }
}
