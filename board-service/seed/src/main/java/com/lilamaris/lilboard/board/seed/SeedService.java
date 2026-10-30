package com.lilamaris.lilboard.board.seed;

import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 요청된 개수만큼 게시판과 게시글을 시딩한다.
 *
 * <p>개수 검증은 저장 전에 완료한다. 검증 실패는 트랜잭션 밖에서 발생하므로 부분 저장된 데이터를 남기지 않는다. 게시판 행이 게시글 행의 외래 키 대상이므로 게시판 먼저
 * 기록하고 게시글을 기록한다.
 */
@RequiredArgsConstructor
public class SeedService {
    private final SeedDataGenerator generator;
    private final SeedStore store;
    private final SeedLimits limits;
    private final Clock clock;

    @Transactional
    public SeedResult seed(int boards, int postsPerBoard) {
        SeedPlan plan = SeedPlan.of(boards, postsPerBoard, limits);
        SeedDataset dataset =
                generator.dataset(plan.boards(), plan.postsPerBoard(), clock.instant());

        store.save(dataset.boards(), dataset.posts());

        return SeedResult.of(dataset.boards().size(), dataset.posts().size());
    }
}
