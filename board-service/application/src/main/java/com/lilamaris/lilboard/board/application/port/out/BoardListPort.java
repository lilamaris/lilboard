package com.lilamaris.lilboard.board.application.port.out;

import com.lilamaris.lilboard.board.domain.Board;
import com.lilamaris.lilboard.kernel.pagination.OffsetPage;
import com.lilamaris.lilboard.kernel.pagination.OffsetPageRequest;

public interface BoardListPort {
    OffsetPage<Board> findAll(OffsetPageRequest pageRequest);
}
