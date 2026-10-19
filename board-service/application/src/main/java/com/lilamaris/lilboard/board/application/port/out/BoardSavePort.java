package com.lilamaris.lilboard.board.application.port.out;

import com.lilamaris.lilboard.board.domain.Board;

public interface BoardSavePort {

    Board save(Board board);
}
