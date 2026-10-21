package com.lilamaris.lilboard.board.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lilamaris.lilboard.board.application.port.in.command.UpdateBoardCommand;
import com.lilamaris.lilboard.board.application.port.in.result.UpdateBoardResult;
import com.lilamaris.lilboard.board.application.port.out.BoardFindPort;
import com.lilamaris.lilboard.board.application.port.out.BoardSavePort;
import com.lilamaris.lilboard.board.domain.Board;
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
class UpdateBoardServiceTest {
    private static final UUID BOARD_ID = UUID.fromString("c9bf9a5c-7a33-4b0e-9c8a-1f2e3d4c5b6a");
    private static final Instant CREATED_AT = Instant.parse("2024-01-15T10:30:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2024-01-15T11:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(UPDATED_AT, ZoneId.of("UTC"));

    @Mock private BoardSavePort boardSavePort;

    @Mock private BoardFindPort boardFindPort;

    private UpdateBoardService service() {
        return new UpdateBoardService(boardSavePort, boardFindPort, FIXED_CLOCK);
    }

    @DisplayName("정상: 명령이 주어지면 찾은 게시물의 이름과 설명을 수정 시각으로 갱신하고 저장 포트가 반환한 게시물의 값으로 결과를 만든다")
    @Test
    void returnsResultBuiltFromUpdatedBoard() {
        // given
        Board board = Board.of(BOARD_ID, "기존 이름", "기존 설명", CREATED_AT);
        when(boardFindPort.findById(BOARD_ID)).thenReturn(Optional.of(board));
        when(boardSavePort.save(any(Board.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UpdateBoardCommand command = new UpdateBoardCommand(BOARD_ID, "새 이름", "새 설명");

        // when
        UpdateBoardResult result = service().update(command);

        // then
        assertThat(result.id()).isEqualTo(BOARD_ID);
        assertThat(result.name()).isEqualTo("새 이름");
        assertThat(result.description()).isEqualTo("새 설명");
        assertThat(result.createdAt()).isEqualTo(CREATED_AT);
        assertThat(result.updatedAt()).isEqualTo(UPDATED_AT);
    }

    @DisplayName("정상: 수정 명령이 주어지면 저장 포트에 수정된 게시물을 전달한다")
    @Test
    void savesUpdatedBoard() {
        // given
        Board board = Board.of(BOARD_ID, "기존 이름", "기존 설명", CREATED_AT);
        when(boardFindPort.findById(BOARD_ID)).thenReturn(Optional.of(board));
        when(boardSavePort.save(any(Board.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UpdateBoardCommand command = new UpdateBoardCommand(BOARD_ID, "새 이름", "새 설명");

        // when
        service().update(command);

        // then
        ArgumentCaptor<Board> captor = ArgumentCaptor.forClass(Board.class);
        verify(boardSavePort).save(captor.capture());

        Board saved = captor.getValue();
        assertThat(saved.getId()).isEqualTo(BOARD_ID);
        assertThat(saved.getName()).isEqualTo("새 이름");
        assertThat(saved.getDescription()).isEqualTo("새 설명");
        assertThat(saved.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(saved.getUpdatedAt()).isEqualTo(UPDATED_AT);
    }

    @DisplayName("실패: 수정 명령이 null이면 NullPointerException을 던진다")
    @Test
    void throwsNullPointerExceptionWhenCommandIsNull() {
        // when & then
        assertThatThrownBy(() -> service().update(null)).isInstanceOf(NullPointerException.class);
    }

    @DisplayName("실패: 해당 id의 게시물이 없으면 IllegalArgumentException을 던진다")
    @Test
    void throwsIllegalArgumentExceptionWhenBoardNotFound() {
        // given
        when(boardFindPort.findById(BOARD_ID)).thenReturn(Optional.empty());

        UpdateBoardCommand command = new UpdateBoardCommand(BOARD_ID, "새 이름", "새 설명");

        // when & then
        assertThatThrownBy(() -> service().update(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Board를 찾을 수 없습니다.");
    }

    @DisplayName("실패: 게시물 저장 포트에서 예외가 발생하면 호출자에게 전파한다")
    @Test
    void propagatesExceptionWhenSavePortFails() {
        // given
        Board board = Board.of(BOARD_ID, "기존 이름", "기존 설명", CREATED_AT);
        when(boardFindPort.findById(BOARD_ID)).thenReturn(Optional.of(board));
        when(boardSavePort.save(any(Board.class))).thenThrow(new RuntimeException("DB 장애"));

        UpdateBoardCommand command = new UpdateBoardCommand(BOARD_ID, "새 이름", "새 설명");

        // when & then
        assertThatThrownBy(() -> service().update(command))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DB 장애");
    }
}
