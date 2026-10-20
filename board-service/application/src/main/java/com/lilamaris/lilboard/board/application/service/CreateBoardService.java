package com.lilamaris.lilboard.board.application.service;

import com.lilamaris.lilboard.board.application.port.in.CreateBoardUseCase;
import com.lilamaris.lilboard.board.application.port.in.command.CreateBoardCommand;
import com.lilamaris.lilboard.board.application.port.in.result.CreateBoardResult;
import com.lilamaris.lilboard.board.application.port.out.BoardSavePort;
import com.lilamaris.lilboard.board.application.port.out.UuidGenerator;
import com.lilamaris.lilboard.board.domain.Board;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Validated
@Service
@RequiredArgsConstructor
public class CreateBoardService implements CreateBoardUseCase {

    private final BoardSavePort boardSavePort;
    private final UuidGenerator uuidGenerator;
    private final Clock clock;

    @Override
    public CreateBoardResult create(CreateBoardCommand command) {
        UUID id = uuidGenerator.generate();
        Instant createdAt = clock.instant();

        Board board = Board.of(id, command.name(), command.description(), createdAt);
        Board saved = boardSavePort.save(board);

        return CreateBoardResult.from(saved);
    }
}
