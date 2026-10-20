package com.lilamaris.lilboard.board.jpa;

import com.lilamaris.lilboard.board.application.port.out.BoardSavePort;
import com.lilamaris.lilboard.board.domain.Board;
import com.lilamaris.lilboard.board.jpa.repository.BoardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BoardSavePortJpaAdapter implements BoardSavePort {

    private final BoardRepository repository;

    @Override
    public Board save(Board board) {
        return repository.save(board);
    }
}
