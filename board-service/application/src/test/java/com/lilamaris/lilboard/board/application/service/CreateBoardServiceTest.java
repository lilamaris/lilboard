package com.lilamaris.lilboard.board.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lilamaris.lilboard.board.application.port.in.command.CreateBoardCommand;
import com.lilamaris.lilboard.board.application.port.in.result.CreateBoardResult;
import com.lilamaris.lilboard.board.application.port.out.BoardSavePort;
import com.lilamaris.lilboard.board.application.port.out.UuidGenerator;
import com.lilamaris.lilboard.board.domain.Board;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateBoardServiceTest {

    private static final UUID BOARD_ID = UUID.fromString("c9bf9a5c-7a33-4b0e-9c8a-1f2e3d4c5b6a");
    private static final Instant CREATED_AT = Instant.parse("2024-01-15T10:30:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(CREATED_AT, ZoneId.of("UTC"));

    @Mock private BoardSavePort boardSavePort;

    @Mock private UuidGenerator uuidGenerator;

    private CreateBoardService service() {
        return new CreateBoardService(boardSavePort, uuidGenerator, FIXED_CLOCK);
    }

    @DisplayName("정상: 명령이 주어지면 저장 포트가 반환한 게시물의 값으로 결과를 만든다")
    @Test
    void returnsResultBuiltFromSavedBoard() {
        // given
        CreateBoardCommand command = new CreateBoardCommand("자유게시판", "자유롭게 이야기를 나눠요");

        when(uuidGenerator.generate()).thenReturn(BOARD_ID);
        when(boardSavePort.save(any(Board.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        CreateBoardResult result = service().create(command);

        // then
        assertThat(result.id()).isEqualTo(BOARD_ID);
        assertThat(result.name()).isEqualTo("자유게시판");
        assertThat(result.description()).isEqualTo("자유롭게 이야기를 나눠요");
        assertThat(result.createdAt()).isEqualTo(CREATED_AT);
    }

    @DisplayName("정상: 제어 가능한 UUID 발급 포트와 시계로 만든 게시물을 저장 포트에 전달한다")
    @Test
    void savesBoardBuiltFromGeneratedIdAndFixedClock() {
        // given
        CreateBoardCommand command = new CreateBoardCommand("자유게시판", "자유롭게 이야기를 나눠요");

        when(uuidGenerator.generate()).thenReturn(BOARD_ID);
        when(boardSavePort.save(any(Board.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        service().create(command);

        // then
        ArgumentCaptor<Board> captor = ArgumentCaptor.forClass(Board.class);
        verify(boardSavePort).save(captor.capture());

        Board saved = captor.getValue();
        assertThat(saved.getId()).isEqualTo(BOARD_ID);
        assertThat(saved.getName()).isEqualTo("자유게시판");
        assertThat(saved.getDescription()).isEqualTo("자유롭게 이야기를 나눠요");
        assertThat(saved.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(saved.getUpdatedAt()).isNull();
    }

    @DisplayName("실패: 게시물 생성 명령이 null이면 NullPointerException을 던진다")
    @Test
    void throwsNullPointerExceptionWhenCommandIsNull() {
        // given
        when(uuidGenerator.generate()).thenReturn(BOARD_ID);

        // when & then
        assertThatThrownBy(() -> service().create(null)).isInstanceOf(NullPointerException.class);
    }

    @DisplayName("실패: 게시물 저장 포트에서 예외가 발생하면 호출자에게 전파한다")
    @Test
    void propagatesExceptionWhenSavePortFails() {
        // given
        CreateBoardCommand command = new CreateBoardCommand("게시판", "설명");

        when(uuidGenerator.generate()).thenReturn(BOARD_ID);
        when(boardSavePort.save(any(Board.class))).thenThrow(new RuntimeException("DB 장애"));

        // when & then
        assertThatThrownBy(() -> service().create(command))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DB 장애");
    }
}
