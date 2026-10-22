package com.lilamaris.lilboard.board.application.port.out;

import com.lilamaris.lilboard.board.domain.Board;
import java.util.Optional;
import java.util.UUID;

public interface BoardFindPort {
    Optional<Board> findById(UUID id);
}
