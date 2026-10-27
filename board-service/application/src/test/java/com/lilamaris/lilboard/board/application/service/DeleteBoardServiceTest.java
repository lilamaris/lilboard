package com.lilamaris.lilboard.board.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.lilamaris.lilboard.board.application.port.in.command.DeleteBoardCommand;
import com.lilamaris.lilboard.board.application.port.out.BoardFindPort;
import com.lilamaris.lilboard.board.application.port.out.BoardSavePort;
import com.lilamaris.lilboard.board.domain.Board;
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
@DisplayName("DeleteBoardService 테스트")
class DeleteBoardServiceTest {
    private static final UUID BOARD_ID = UUID.fromString("c9bf9a5c-7a33-4b0e-9c8a-1f2e3d4c5b6a");
    private static final Instant CREATED_AT = Instant.parse("2024-01-15T10:30:00Z");
    private static final Instant DELETED_AT = Instant.parse("2024-01-15T11:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(DELETED_AT, ZoneId.of("UTC"));

    @Mock private BoardSavePort boardSavePort;

    @Mock private BoardFindPort boardFindPort;

    private DeleteBoardService service() {
        return new DeleteBoardService(boardSavePort, boardFindPort, FIXED_CLOCK);
    }

    @DisplayName("정상: 명령이 주어지면 찾은 게시판을 클럭 시각으로 삭제하고 저장 포트에 전달한다")
    @Test
    void deletesFoundBoardWithClockTimeAndSavesIt() {
        // given
        Board board = Board.of(BOARD_ID, "게시판", "설명", CREATED_AT);
        when(boardFindPort.findById(BOARD_ID)).thenReturn(Optional.of(board));
        when(boardSavePort.save(any(Board.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        service().delete(new DeleteBoardCommand(BOARD_ID));

        // then
        ArgumentCaptor<Board> captor = ArgumentCaptor.forClass(Board.class);
        verify(boardSavePort).save(captor.capture());

        Board saved = captor.getValue();
        assertThat(saved.getId()).isEqualTo(BOARD_ID);
        assertThat(saved.getName()).isEqualTo("게시판");
        assertThat(saved.getDescription()).isEqualTo("설명");
        assertThat(saved.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(saved.getUpdatedAt()).isNull();
        assertThat(saved.getDeletedAt()).isEqualTo(DELETED_AT);
    }

    @Test
    @DisplayName("클럭 시각이 생성 시각보다 빠르면 예외를 전파하고 삭제 상태를 변경하거나 저장하지 않는다")
    void rejectsDeletionBeforeCreationWithoutSaving() {
        Board board = Board.of(BOARD_ID, "게시판", "설명", CREATED_AT);
        when(boardFindPort.findById(BOARD_ID)).thenReturn(Optional.of(board));
        Clock earlierClock = Clock.fixed(CREATED_AT.minusNanos(1), ZoneId.of("UTC"));
        DeleteBoardService service =
                new DeleteBoardService(boardSavePort, boardFindPort, earlierClock);

        assertThatThrownBy(() -> service.delete(new DeleteBoardCommand(BOARD_ID)))
                .isInstanceOf(IllegalComparisonConditionException.class);

        assertThat(board.getDeletedAt()).isNull();
        verifyNoInteractions(boardSavePort);
    }

    @DisplayName("해당 ID의 게시판이 없으면 IllegalArgumentException을 던지고 저장하지 않는다")
    @Test
    void throwsIllegalArgumentExceptionWhenBoardNotFound() {
        // given
        when(boardFindPort.findById(BOARD_ID)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service().delete(new DeleteBoardCommand(BOARD_ID)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Board를 찾을 수 없습니다.");

        verifyNoInteractions(boardSavePort);
    }

    @DisplayName("실패: 삭제 명령이 null이면 NullPointerException을 던지고 포트를 호출하지 않는다")
    @Test
    void throwsNullPointerExceptionWhenCommandIsNull() {
        // when & then
        assertThatThrownBy(() -> service().delete(null)).isInstanceOf(NullPointerException.class);

        verifyNoInteractions(boardFindPort, boardSavePort);
    }

    @DisplayName("게시판 저장 포트에서 예외가 발생하면 호출자에게 전파한다")
    @Test
    void propagatesExceptionWhenSavePortFails() {
        // given
        Board board = Board.of(BOARD_ID, "게시판", "설명", CREATED_AT);
        when(boardFindPort.findById(BOARD_ID)).thenReturn(Optional.of(board));
        when(boardSavePort.save(any(Board.class))).thenThrow(new RuntimeException("DB 장애"));

        // when & then
        assertThatThrownBy(() -> service().delete(new DeleteBoardCommand(BOARD_ID)))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DB 장애");
    }
}
