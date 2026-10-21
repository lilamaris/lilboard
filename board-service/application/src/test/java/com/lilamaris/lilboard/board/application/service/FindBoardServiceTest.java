package com.lilamaris.lilboard.board.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.lilamaris.lilboard.board.application.port.in.query.FindBoardQuery;
import com.lilamaris.lilboard.board.application.port.in.result.FindBoardResult;
import com.lilamaris.lilboard.board.application.port.out.BoardFindPort;
import com.lilamaris.lilboard.board.domain.Board;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FindBoardServiceTest {
    private static final UUID BOARD_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-01-02T00:00:00Z");

    @Mock private BoardFindPort boardFindPort;

    @Test
    @DisplayName("게시판이 존재하면 게시판 정보와 비어 있는 수정 시각을 반환한다")
    void returnsBoardWithoutUpdatedAt() {
        Board board = Board.of(BOARD_ID, "게시판", "설명", CREATED_AT);
        when(boardFindPort.findById(BOARD_ID)).thenReturn(Optional.of(board));

        var result = new FindBoardService(boardFindPort).find(new FindBoardQuery(BOARD_ID));

        assertThat(result).isEqualTo(new FindBoardResult(BOARD_ID, "게시판", "설명", CREATED_AT, null));
    }

    @Test
    @DisplayName("수정된 게시판이면 변경된 정보와 수정 시각을 반환한다")
    void returnsUpdatedBoard() {
        Board board = Board.of(BOARD_ID, "게시판", "설명", CREATED_AT);
        board.updateName("수정 게시판", UPDATED_AT);
        board.updateDescription("수정 설명", UPDATED_AT);
        when(boardFindPort.findById(BOARD_ID)).thenReturn(Optional.of(board));

        var result = new FindBoardService(boardFindPort).find(new FindBoardQuery(BOARD_ID));

        assertThat(result)
                .isEqualTo(
                        new FindBoardResult(BOARD_ID, "수정 게시판", "수정 설명", CREATED_AT, UPDATED_AT));
    }

    @Test
    @DisplayName("게시판이 존재하지 않으면 IllegalArgumentException을 던진다")
    void rejectsMissingBoard() {
        when(boardFindPort.findById(BOARD_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                new FindBoardService(boardFindPort)
                                        .find(new FindBoardQuery(BOARD_ID)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Board를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("조회 포트에서 장애가 발생하면 예외를 호출자에게 전파한다")
    void propagatesFindFailure() {
        var failure = new IllegalStateException("DB 장애");
        when(boardFindPort.findById(BOARD_ID)).thenThrow(failure);

        assertThatThrownBy(
                        () ->
                                new FindBoardService(boardFindPort)
                                        .find(new FindBoardQuery(BOARD_ID)))
                .isSameAs(failure);
    }
}
