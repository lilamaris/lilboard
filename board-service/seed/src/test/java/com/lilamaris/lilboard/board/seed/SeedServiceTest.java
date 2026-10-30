package com.lilamaris.lilboard.board.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.github.f4b6a3.uuid.UuidCreator;
import com.lilamaris.lilboard.board.domain.Board;
import com.lilamaris.lilboard.board.domain.Post;
import com.lilamaris.lilboard.kernel.exception.IllegalComparisonConditionException;
import com.lilamaris.lilboard.kernel.exception.IllegalValueConditionException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@DisplayName("SeedService 테스트")
class SeedServiceTest {
    private static final Instant BASE = Instant.parse("2026-01-01T00:00:00Z");
    private static final SeedLimits LIMITS = new SeedLimits(100_000, 500);

    private SeedStore store;
    private SeedService service;
    private ArgumentCaptor<List<Board>> boards;
    private ArgumentCaptor<List<Post>> posts;

    @BeforeEach
    void setUp() {
        store = mock(SeedStore.class);
        service =
                new SeedService(
                        new SeedDataGenerator(UuidCreator::getTimeOrderedEpoch),
                        store,
                        LIMITS,
                        Clock.fixed(BASE, ZoneOffset.UTC));
        boards = listCaptor();
        posts = listCaptor();
    }

    @Test
    @DisplayName("게시판과 게시글을 시딩하면 요청한 개수만큼 저장하고 결과를 반환한다")
    void savesRequestedCountsAndReturnsResults() {
        SeedResult result = service.seed(3, 2);

        assertThat(result.boards()).isEqualTo(3);
        assertThat(result.posts()).isEqualTo(6);
        assertThat(result.totalRows()).isEqualTo(9);

        verify(store).save(boards.capture(), posts.capture());
        List<UUID> boardIds = boards.getValue().stream().map(Board::getId).toList();
        assertThat(boards.getValue()).hasSize(3);
        assertThat(posts.getValue()).hasSize(6);
        assertThat(posts.getValue())
                .allSatisfy(post -> assertThat(boardIds).contains(post.getBoardId()));
    }

    @Test
    @DisplayName("게시글 개수가 0이면 게시판 행만 저장한다")
    void savesOnlyBoardRowsWhenPostsPerBoardIsZero() {
        SeedResult result = service.seed(2, 0);

        assertThat(result.boards()).isEqualTo(2);
        assertThat(result.posts()).isZero();

        verify(store).save(boards.capture(), posts.capture());
        assertThat(boards.getValue()).hasSize(2);
        assertThat(posts.getValue()).isEmpty();
    }

    @Test
    @DisplayName("시딩 시각은 주입된 Clock의 현재 시각을 마지막 행의 생성 시각으로 사용한다")
    void usesInjectedClockInstantAsLatestCreatedAt() {
        service.seed(2, 1);

        verify(store).save(boards.capture(), posts.capture());
        Instant latestPost = posts.getValue().getLast().getCreatedAt();
        assertThat(latestPost).isEqualTo(BASE);
        assertThat(boards.getValue().getFirst().getCreatedAt()).isBefore(latestPost);
    }

    @Test
    @DisplayName("총 행이 상한을 넘으면 저장소에 접근하지 않는다")
    void doesNotTouchStoreWhenTotalRowsExceedUpperBound() {
        assertThatThrownBy(() -> service.seed(100_001, 0))
                .isInstanceOf(IllegalComparisonConditionException.class);

        verifyNoInteractions(store);
    }

    @Test
    @DisplayName("게시판 개수가 0 이하이면 저장소에 접근하지 않는다")
    void doesNotTouchStoreWhenBoardCountIsNotPositive() {
        assertThatThrownBy(() -> service.seed(0, 10))
                .isInstanceOf(IllegalValueConditionException.class);

        verifyNoInteractions(store);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> ArgumentCaptor<List<T>> listCaptor() {
        return ArgumentCaptor.forClass(List.class);
    }
}
