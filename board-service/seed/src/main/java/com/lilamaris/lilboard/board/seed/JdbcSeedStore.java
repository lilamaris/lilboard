package com.lilamaris.lilboard.board.seed;

import com.lilamaris.lilboard.board.domain.Board;
import com.lilamaris.lilboard.board.domain.Post;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 시딩 데이터를 JDBC 배치로 기록한다.
 *
 * <p>부하 테스트 규모(수만~수십만 행)에서는 엔티티 단위 저장의 persistence context 유지비가 커지므로 JDBC 배치로 저장한다. 컬럼은 Flyway
 * 스키마(board, post)와 엔티티 필드를 함께 확인해 유지한다.
 */
@RequiredArgsConstructor
public final class JdbcSeedStore implements SeedStore {
    private static final String BOARD_INSERT =
            "INSERT INTO board (id, name, description, created_at) VALUES (?, ?, ?, ?)";
    private static final String POST_INSERT =
            "INSERT INTO post (id, board_id, title, content, created_at) VALUES (?, ?, ?, ?, ?)";

    private final JdbcTemplate jdbcTemplate;
    private final SeedLimits limits;

    @Override
    public void save(List<Board> boards, List<Post> posts) {
        insert(BOARD_INSERT, boards, JdbcSeedStore::boardArgs);
        insert(POST_INSERT, posts, JdbcSeedStore::postArgs);
    }

    private <T> void insert(String sql, List<T> rows, Function<T, Object[]> argsMapper) {
        List<Object[]> args = rows.stream().map(argsMapper).toList();
        int batchSize = limits.batchSize();
        for (int from = 0; from < args.size(); from += batchSize) {
            int to = Math.min(from + batchSize, args.size());
            jdbcTemplate.batchUpdate(sql, args.subList(from, to));
        }
    }

    private static Object[] boardArgs(Board board) {
        return new Object[] {
            board.getId(), board.getName(), board.getDescription(), timestamp(board.getCreatedAt())
        };
    }

    private static Object[] postArgs(Post post) {
        return new Object[] {
            post.getId(),
            post.getBoardId(),
            post.getTitle(),
            post.getContent(),
            timestamp(post.getCreatedAt())
        };
    }

    private static Timestamp timestamp(Instant createdAt) {
        return Timestamp.from(createdAt);
    }
}
