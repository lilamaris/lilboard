package com.lilamaris.lilboard.board.seed;

import com.lilamaris.lilboard.board.application.port.out.UuidGenerator;
import com.lilamaris.lilboard.board.domain.Board;
import com.lilamaris.lilboard.board.domain.Post;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 시딩 데이터 세트는 행마다 1초 간격으로 생성 시각을 부여한다.
 *
 * <p>생성 순서는 게시판, 그 게시판의 게시글이고 생성 시각은 oldest-first로 부여된다. 게시글은 자기 게시판을 참조하므로 게시판보다 먼저 생성되지 않고, 부여된
 * 시각은 시딩 시각(base)를 넘지 않는다.
 */
final class SeedDataGenerator {
    private static final int INDEX_DIGITS = 6;
    private static final String BOARD_DESCRIPTION = "loadtest board description";
    private static final String POST_CONTENT = "loadtest post content";

    private final UuidGenerator uuidGenerator;

    SeedDataGenerator(UuidGenerator uuidGenerator) {
        this.uuidGenerator = uuidGenerator;
    }

    /**
     * @param boards 생성할 게시판 개수
     * @param postsPerBoard 각 게시판의 게시글 개수
     * @param base 가장 나중에 생성되는 행의 생성 시각
     */
    SeedDataset dataset(int boards, int postsPerBoard, Instant base) {
        long totalRows = (long) boards * (1 + postsPerBoard);
        List<Board> boardRows = new ArrayList<>(boards);
        List<Post> postRows = new ArrayList<>((int) (totalRows - boards));

        long index = 0;
        for (int boardIndex = 0; boardIndex < boards; boardIndex++) {
            Board board =
                    Board.of(
                            uuidGenerator.generate(),
                            boardName(boardIndex),
                            boardDescription(boardIndex),
                            createdAt(base, totalRows, index));
            boardRows.add(board);
            index++;

            for (int postIndex = 0; postIndex < postsPerBoard; postIndex++) {
                postRows.add(
                        Post.of(
                                uuidGenerator.generate(),
                                board.getId(),
                                postTitle(boardIndex, postIndex),
                                postContent(boardIndex, postIndex),
                                createdAt(base, totalRows, index)));
                index++;
            }
        }

        return new SeedDataset(List.copyOf(boardRows), List.copyOf(postRows));
    }

    private static Instant createdAt(Instant base, long totalRows, long index) {
        return base.minusSeconds(totalRows - 1 - index);
    }

    private static String boardName(int boardIndex) {
        return String.format("board-%0" + INDEX_DIGITS + "d", boardIndex);
    }

    private static String boardDescription(int boardIndex) {
        return BOARD_DESCRIPTION + " " + boardName(boardIndex);
    }

    private static String postTitle(int boardIndex, int postIndex) {
        return String.format(
                "post-%0" + INDEX_DIGITS + "d-%0" + INDEX_DIGITS + "d", boardIndex, postIndex);
    }

    private static String postContent(int boardIndex, int postIndex) {
        return POST_CONTENT + " " + postTitle(boardIndex, postIndex);
    }
}
