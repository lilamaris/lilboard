package com.lilamaris.lilboard.board.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.lilamaris.lilboard.board.application.port.in.query.FindPostsQuery;
import com.lilamaris.lilboard.board.application.port.in.result.FindPostResult;
import com.lilamaris.lilboard.board.application.port.out.PostListPort;
import com.lilamaris.lilboard.board.domain.Post;
import com.lilamaris.lilboard.kernel.pagination.OffsetPage;
import com.lilamaris.lilboard.kernel.pagination.OffsetPageRequest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FindPostsServiceTest {
    @Mock private PostListPort postListPort;

    @Test
    @DisplayName("목록 조회 시 항목 순서와 게시글 정보 및 페이지 정보를 유지해 반환한다")
    void returnsPostPage() {
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        Instant updatedAt = Instant.parse("2026-01-02T00:00:00Z");
        UUID boardId = UUID.fromString("00000000-0000-0000-0000-000000000003");
        UUID firstId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID secondId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        Post first = Post.of(firstId, boardId, "제목 A", "본문 A", createdAt);
        first.updateTitle("수정 제목 A", updatedAt);
        Post second = Post.of(secondId, boardId, "제목 B", "본문 B", createdAt);
        var request = OffsetPageRequest.of(1, 2);
        when(postListPort.findAllVisible(request))
                .thenReturn(OffsetPage.of(List.of(first, second), 1, 2, 5));

        var result = new FindPostsService(postListPort).find(new FindPostsQuery(request));

        assertThat(result)
                .isEqualTo(
                        OffsetPage.of(
                                List.of(
                                        new FindPostResult(
                                                firstId, boardId, "수정 제목 A", "본문 A", createdAt,
                                                updatedAt),
                                        new FindPostResult(
                                                secondId, boardId, "제목 B", "본문 B", createdAt,
                                                null)),
                                1,
                                2,
                                5));
    }

    @Test
    @DisplayName("게시글이 없으면 빈 페이지와 전체 개수 0을 반환한다")
    void returnsEmptyPage() {
        var request = OffsetPageRequest.of(0, 20);
        when(postListPort.findAllVisible(request)).thenReturn(OffsetPage.of(List.of(), 0, 20, 0));

        var result = new FindPostsService(postListPort).find(new FindPostsQuery(request));

        assertThat(result).isEqualTo(OffsetPage.of(List.of(), 0, 20, 0));
    }

    @Test
    @DisplayName("마지막 페이지를 벗어난 요청이면 빈 항목과 기존 전체 개수를 반환한다")
    void returnsPageBeyondLast() {
        var request = OffsetPageRequest.of(3, 20);
        when(postListPort.findAllVisible(request)).thenReturn(OffsetPage.of(List.of(), 3, 20, 5));

        var result = new FindPostsService(postListPort).find(new FindPostsQuery(request));

        assertThat(result).isEqualTo(OffsetPage.of(List.of(), 3, 20, 5));
    }

    @Test
    @DisplayName("목록 조회 포트의 장애를 호출자에게 전파한다")
    void propagatesListFailure() {
        var request = OffsetPageRequest.of(0, 20);
        var failure = new IllegalStateException("DB 장애");
        when(postListPort.findAllVisible(request)).thenThrow(failure);

        assertThatThrownBy(
                        () -> new FindPostsService(postListPort).find(new FindPostsQuery(request)))
                .isSameAs(failure);
    }
}
