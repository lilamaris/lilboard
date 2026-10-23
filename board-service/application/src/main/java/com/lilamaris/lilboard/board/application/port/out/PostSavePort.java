package com.lilamaris.lilboard.board.application.port.out;

import com.lilamaris.lilboard.board.domain.Post;

public interface PostSavePort {
    Post save(Post post);
}
