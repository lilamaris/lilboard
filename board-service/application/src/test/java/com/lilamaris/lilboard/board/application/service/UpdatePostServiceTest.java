package com.lilamaris.lilboard.board.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.lilamaris.lilboard.board.application.port.in.command.UpdatePostCommand;
import com.lilamaris.lilboard.board.application.port.in.result.UpdatePostResult;
import com.lilamaris.lilboard.board.application.port.out.PostFindPort;
import com.lilamaris.lilboard.board.application.port.out.PostSavePort;
import com.lilamaris.lilboard.board.domain.Post;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UpdatePostServiceTest {
    private static final UUID POST_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID BOARD_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant UPDATED_AT = CREATED_AT.plusSeconds(1);
    private static final Clock FIXED_CLOCK = Clock.fixed(UPDATED_AT, ZoneOffset.UTC);

    @Mock private PostSavePort postSavePort;

    @Mock private PostFindPort postFindPort;

    private UpdatePostService service() {
        return new UpdatePostService(postSavePort, postFindPort, FIXED_CLOCK);
    }

    @Test
    @DisplayName("수정 명령이 주어지면 제목·본문과 수정 시각을 갱신한 게시글을 저장하고 결과를 반환한다")
    void savesUpdatedPostAndReturnsResult() {
        Post post = Post.of(POST_ID, BOARD_ID, "기존 제목", "기존 본문", CREATED_AT);
        stubVisiblePost(post);
        when(postSavePort.save(any(Post.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UpdatePostResult result =
                service().update(new UpdatePostCommand(POST_ID, "새 제목", "첫 번째 줄\n두 번째 줄"));

        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        verify(postSavePort).save(captor.capture());
        Post saved = captor.getValue();
        assertThat(saved.getId()).isEqualTo(POST_ID);
        assertThat(saved.getBoardId()).isEqualTo(BOARD_ID);
        assertThat(saved.getTitle()).isEqualTo("새 제목");
        assertThat(saved.getContent()).isEqualTo("첫 번째 줄\n두 번째 줄");
        assertThat(saved.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(saved.getUpdatedAt()).isEqualTo(UPDATED_AT);
        assertThat(result)
                .isEqualTo(
                        new UpdatePostResult(
                                POST_ID,
                                BOARD_ID,
                                "새 제목",
                                "첫 번째 줄\n두 번째 줄",
                                CREATED_AT,
                                UPDATED_AT));
    }

    @Test
    @DisplayName("저장 포트가 별도의 게시글을 반환하면 해당 게시글의 값으로 결과를 만든다")
    void returnsResultBuiltFromSavedPost() {
        Post post = Post.of(POST_ID, BOARD_ID, "기존 제목", "기존 본문", CREATED_AT);
        Post saved = Post.of(POST_ID, BOARD_ID, "저장된 제목", "저장된 본문", CREATED_AT);
        Instant savedAt = UPDATED_AT.plusSeconds(1);
        saved.updateContent("저장된 본문", savedAt);
        stubVisiblePost(post);
        when(postSavePort.save(any(Post.class))).thenReturn(saved);

        UpdatePostResult result = service().update(new UpdatePostCommand(POST_ID, "새 제목", "새 본문"));

        assertThat(result)
                .isEqualTo(
                        new UpdatePostResult(
                                POST_ID, BOARD_ID, "저장된 제목", "저장된 본문", CREATED_AT, savedAt));
    }

    @Test
    @DisplayName("가시 게시글이 조회되지 않으면 예외를 던지고 저장하지 않는다")
    void throwsWhenPostIsNotVisibleWithoutSaving() {
        when(postFindPort.findVisibleById(POST_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().update(new UpdatePostCommand(POST_ID, "새 제목", "새 본문")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Post를 찾을 수 없습니다.");
        verifyNoInteractions(postSavePort);
    }

    @Test
    @DisplayName("게시글 조회 포트에서 예외가 발생하면 호출자에게 전파하고 저장하지 않는다")
    void propagatesFindFailureWithoutSaving() {
        RuntimeException failure = new RuntimeException("조회 장애");
        when(postFindPort.findVisibleById(POST_ID)).thenThrow(failure);

        assertThatThrownBy(() -> service().update(new UpdatePostCommand(POST_ID, "새 제목", "새 본문")))
                .isSameAs(failure);
        verifyNoInteractions(postSavePort);
    }

    @Test
    @DisplayName("게시글 저장 포트에서 예외가 발생하면 호출자에게 전파한다")
    void propagatesSaveFailure() {
        Post post = Post.of(POST_ID, BOARD_ID, "기존 제목", "기존 본문", CREATED_AT);
        RuntimeException failure = new RuntimeException("저장 장애");
        stubVisiblePost(post);
        when(postSavePort.save(any(Post.class))).thenThrow(failure);

        assertThatThrownBy(() -> service().update(new UpdatePostCommand(POST_ID, "새 제목", "새 본문")))
                .isSameAs(failure);
    }

    @Test
    @DisplayName("수정 명령이 null이면 NullPointerException을 던지고 외부 의존성을 호출하지 않는다")
    void rejectsNullCommand() {
        assertThatThrownBy(() -> service().update(null)).isInstanceOf(NullPointerException.class);
        verifyNoInteractions(postFindPort, postSavePort);
    }

    private void stubVisiblePost(Post post) {
        when(postFindPort.findVisibleById(POST_ID)).thenReturn(Optional.of(post));
    }
}
