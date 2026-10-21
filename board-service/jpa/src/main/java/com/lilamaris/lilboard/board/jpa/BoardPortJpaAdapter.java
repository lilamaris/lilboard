package com.lilamaris.lilboard.board.jpa;

import com.lilamaris.lilboard.board.application.port.out.BoardFindPort;
import com.lilamaris.lilboard.board.application.port.out.BoardSavePort;
import com.lilamaris.lilboard.board.domain.Board;
import com.lilamaris.lilboard.board.jpa.repository.BoardRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BoardPortJpaAdapter implements BoardSavePort, BoardFindPort {
    private final BoardRepository repository;

    @Override
    public Board save(Board board) {
        return repository.save(board);
    }

    @Override
    public Optional<Board> findById(UUID id) {
        return repository.findById(id);
    }
}
