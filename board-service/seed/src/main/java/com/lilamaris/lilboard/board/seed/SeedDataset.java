package com.lilamaris.lilboard.board.seed;

import com.lilamaris.lilboard.board.domain.Board;
import com.lilamaris.lilboard.board.domain.Post;
import java.util.List;

/** 시딩할 게시판·게시글 행 세트. 생성 순서가 게시판, 그 게시판의 게시글 순서이고 전체 행 수를 알린다. */
public record SeedDataset(List<Board> boards, List<Post> posts) {
    public int size() {
        return boards.size() + posts.size();
    }
}
