package com.lilamaris.lilboard.board.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.lilamaris.lilboard.board.application.port.in.query.FindBoardsQuery;
import com.lilamaris.lilboard.board.application.port.in.result.FindBoardResult;
import com.lilamaris.lilboard.board.application.port.out.BoardListPort;
import com.lilamaris.lilboard.board.domain.Board;
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
class FindBoardsServiceTest {
    @Mock private BoardListPort boardListPort;

    @Test
    @DisplayName("목록 조회 시 항목 순서와 게시판 정보 및 페이지 정보를 유지해 반환한다")
    void returnsBoardPage() {
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        Instant updatedAt = Instant.parse("2026-01-02T00:00:00Z");
        UUID firstId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID secondId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        Board first = Board.of(firstId, "게시판 A", "설명 A", createdAt);
        first.updateName("수정 게시판 A", updatedAt);
        Board second = Board.of(secondId, "게시판 B", "설명 B", createdAt);
        var request = OffsetPageRequest.of(1, 2);
        when(boardListPort.findAll(request))
                .thenReturn(OffsetPage.of(List.of(first, second), 1, 2, 5));

        var result = new FindBoardsService(boardListPort).find(new FindBoardsQuery(request));

        assertThat(result)
                .isEqualTo(
                        OffsetPage.of(
                                List.of(
                                        new FindBoardResult(
                                                firstId, "수정 게시판 A", "설명 A", createdAt, updatedAt),
                                        new FindBoardResult(
                                                secondId, "게시판 B", "설명 B", createdAt, null)),
                                1,
                                2,
                                5));
    }

    @Test
    @DisplayName("게시판이 없으면 빈 페이지와 전체 개수 0을 반환한다")
    void returnsEmptyPage() {
        var request = OffsetPageRequest.of(0, 20);
        when(boardListPort.findAll(request)).thenReturn(OffsetPage.of(List.of(), 0, 20, 0));
        var result = new FindBoardsService(boardListPort).find(new FindBoardsQuery(request));
        assertThat(result).isEqualTo(OffsetPage.of(List.of(), 0, 20, 0));
    }

    @Test
    @DisplayName("마지막 페이지를 벗어난 요청이면 빈 항목과 기존 전체 개수를 반환한다")
    void returnsPageBeyondLast() {
        var request = OffsetPageRequest.of(3, 20);
        when(boardListPort.findAll(request)).thenReturn(OffsetPage.of(List.of(), 3, 20, 5));
        var result = new FindBoardsService(boardListPort).find(new FindBoardsQuery(request));
        assertThat(result).isEqualTo(OffsetPage.of(List.of(), 3, 20, 5));
    }

    @Test
    @DisplayName("목록 조회 포트의 장애를 호출자에게 전파한다")
    void propagatesListFailure() {
        var request = OffsetPageRequest.of(0, 20);
        var failure = new IllegalStateException("DB 장애");
        when(boardListPort.findAll(request)).thenThrow(failure);
        assertThatThrownBy(
                        () ->
                                new FindBoardsService(boardListPort)
                                        .find(new FindBoardsQuery(request)))
                .isSameAs(failure);
    }
}
