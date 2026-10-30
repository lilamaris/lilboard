package com.lilamaris.lilboard.board.seed;

import com.lilamaris.lilboard.board.domain.Board;
import com.lilamaris.lilboard.board.domain.Post;
import java.util.List;

/** 시딩 데이터를 저장소에 기록하는 경계. */
public interface SeedStore {
    void save(List<Board> boards, List<Post> posts);
}
