package com.lilamaris.lilboard.board.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.lilamaris.lilboard.board.application.port.in.query.FindPostQuery;
import com.lilamaris.lilboard.board.application.port.in.result.FindPostResult;
import com.lilamaris.lilboard.board.application.port.out.PostFindPort;
import com.lilamaris.lilboard.board.domain.Post;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FindPostServiceTest {
    private static final UUID POST_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID BOARD_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-01-02T00:00:00Z");

    @Mock private PostFindPort postFindPort;

    @Test
    @DisplayName("가시 게시글이 조회되면 게시글 정보와 비어 있는 수정 시각을 반환한다")
    void returnsPostWithoutUpdatedAt() {
        Post post = Post.of(POST_ID, BOARD_ID, "제목", "첫 번째 줄\n두 번째 줄", CREATED_AT);
        stubVisiblePost(post);

        var result = new FindPostService(postFindPort).find(new FindPostQuery(POST_ID));

        assertThat(result)
                .isEqualTo(
                        new FindPostResult(
                                POST_ID, BOARD_ID, "제목", "첫 번째 줄\n두 번째 줄", CREATED_AT, null));
    }

    @Test
    @DisplayName("수정된 게시글이면 변경된 정보와 수정 시각을 반환한다")
    void returnsUpdatedPost() {
        Post post = Post.of(POST_ID, BOARD_ID, "제목", "본문", CREATED_AT);
        post.updateTitle("수정 제목", UPDATED_AT);
        post.updateContent("수정 본문", UPDATED_AT);
        stubVisiblePost(post);

        var result = new FindPostService(postFindPort).find(new FindPostQuery(POST_ID));

        assertThat(result)
                .isEqualTo(
                        new FindPostResult(
                                POST_ID, BOARD_ID, "수정 제목", "수정 본문", CREATED_AT, UPDATED_AT));
    }

    @Test
    @DisplayName("가시 게시글이 조회되지 않으면 IllegalArgumentException을 던진다")
    void rejectsInvisiblePost() {
        when(postFindPort.findVisibleById(POST_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new FindPostService(postFindPort).find(new FindPostQuery(POST_ID)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Post를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("조회 포트에서 장애가 발생하면 예외를 호출자에게 전파한다")
    void propagatesFindFailure() {
        var failure = new IllegalStateException("DB 장애");
        when(postFindPort.findVisibleById(POST_ID)).thenThrow(failure);

        assertThatThrownBy(() -> new FindPostService(postFindPort).find(new FindPostQuery(POST_ID)))
                .isSameAs(failure);
    }

    private void stubVisiblePost(Post post) {
        when(postFindPort.findVisibleById(POST_ID)).thenReturn(Optional.of(post));
    }
}
