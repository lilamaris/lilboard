package com.lilamaris.lilboard.board.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.lilamaris.lilboard.board.application.port.in.command.CreatePostCommand;
import com.lilamaris.lilboard.board.application.port.in.result.CreatePostResult;
import com.lilamaris.lilboard.board.application.port.out.BoardFindPort;
import com.lilamaris.lilboard.board.application.port.out.PostSavePort;
import com.lilamaris.lilboard.board.application.port.out.UuidGenerator;
import com.lilamaris.lilboard.board.domain.Board;
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
class CreatePostServiceTest {
    private static final UUID POST_ID = UUID.fromString("c9bf9a5c-7a33-4b0e-9c8a-1f2e3d4c5b6a");
    private static final UUID BOARD_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final Instant CREATED_AT = Instant.parse("2024-01-15T10:30:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(CREATED_AT, ZoneId.of("UTC"));

    @Mock private BoardFindPort boardFindPort;

    @Mock private PostSavePort postSavePort;

    @Mock private UuidGenerator uuidGenerator;

    private CreatePostService service() {
        return new CreatePostService(boardFindPort, postSavePort, uuidGenerator, FIXED_CLOCK);
    }

    @DisplayName("정상: 조회 가능한 게시판의 게시글을 저장 포트가 반환한 값으로 결과를 만든다")
    @Test
    void returnsResultBuiltFromSavedPost() {
        // given
        CreatePostCommand command = new CreatePostCommand(BOARD_ID, "게시글 제목", "첫 번째 줄\n두 번째 줄");

        stubVisibleBoard();
        when(uuidGenerator.generate()).thenReturn(POST_ID);
        when(postSavePort.save(any(Post.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        CreatePostResult result = service().create(command);

        // then
        assertThat(result.id()).isEqualTo(POST_ID);
        assertThat(result.boardId()).isEqualTo(BOARD_ID);
        assertThat(result.title()).isEqualTo("게시글 제목");
        assertThat(result.content()).isEqualTo("첫 번째 줄\n두 번째 줄");
        assertThat(result.createdAt()).isEqualTo(CREATED_AT);
    }

    @DisplayName("정상: 제어 가능한 UUID 발급 포트와 시계로 만든 게시글을 저장 포트에 전달한다")
    @Test
    void savesPostBuiltFromGeneratedIdAndFixedClock() {
        // given
        CreatePostCommand command = new CreatePostCommand(BOARD_ID, "게시글 제목", "본문");

        stubVisibleBoard();
        when(uuidGenerator.generate()).thenReturn(POST_ID);
        when(postSavePort.save(any(Post.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        service().create(command);

        // then
        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        verify(postSavePort).save(captor.capture());

        Post saved = captor.getValue();
        assertThat(saved.getId()).isEqualTo(POST_ID);
        assertThat(saved.getBoardId()).isEqualTo(BOARD_ID);
        assertThat(saved.getTitle()).isEqualTo("게시글 제목");
        assertThat(saved.getContent()).isEqualTo("본문");
        assertThat(saved.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(saved.getUpdatedAt()).isNull();
    }

    @DisplayName("실패: 조회 가능한 게시판이 없으면 예외를 던지고 UUID를 발급하지 않고 저장하지 않는다")
    @Test
    void rejectsCreateWhenBoardIsNotVisible() {
        // given
        CreatePostCommand command = new CreatePostCommand(BOARD_ID, "제목", "본문");

        when(boardFindPort.findById(BOARD_ID)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service().create(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Board를 찾을 수 없습니다.");
        verifyNoInteractions(uuidGenerator, postSavePort);
    }

    @DisplayName("실패: 게시글 생성 명령이 null이면 NullPointerException을 던지고 게시판 조회·저장하지 않는다")
    @Test
    void throwsNullPointerExceptionWhenCommandIsNull() {
        // when & then
        assertThatThrownBy(() -> service().create(null)).isInstanceOf(NullPointerException.class);
        verifyNoInteractions(boardFindPort, uuidGenerator, postSavePort);
    }

    @DisplayName("실패: 게시글 저장 포트에서 예외가 발생하면 호출자에게 전파한다")
    @Test
    void propagatesExceptionWhenSavePortFails() {
        // given
        CreatePostCommand command = new CreatePostCommand(BOARD_ID, "제목", "본문");

        stubVisibleBoard();
        when(uuidGenerator.generate()).thenReturn(POST_ID);
        when(postSavePort.save(any(Post.class))).thenThrow(new RuntimeException("DB 장애"));

        // when & then
        assertThatThrownBy(() -> service().create(command))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DB 장애");
    }

    private void stubVisibleBoard() {
        when(boardFindPort.findById(BOARD_ID))
                .thenReturn(Optional.of(Board.of(BOARD_ID, "게시판", "설명", CREATED_AT)));
    }
}
