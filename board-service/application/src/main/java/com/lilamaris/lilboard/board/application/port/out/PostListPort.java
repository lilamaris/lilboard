package com.lilamaris.lilboard.board.application.port.out;

import com.lilamaris.lilboard.board.domain.Post;
import com.lilamaris.lilboard.kernel.pagination.OffsetPage;
import com.lilamaris.lilboard.kernel.pagination.OffsetPageRequest;

public interface PostListPort {
    OffsetPage<Post> findAll(OffsetPageRequest pageRequest);
}
