package com.lilamaris.lilboard.board.application.service;

import com.lilamaris.lilboard.board.application.port.in.DeleteBoardUseCase;
import com.lilamaris.lilboard.board.application.port.in.command.DeleteBoardCommand;
import com.lilamaris.lilboard.board.application.port.out.BoardFindPort;
import com.lilamaris.lilboard.board.application.port.out.BoardSavePort;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Validated
@Service
@RequiredArgsConstructor
public class DeleteBoardService implements DeleteBoardUseCase {
    private final BoardSavePort boardSavePort;
    private final BoardFindPort boardFindPort;
    private final Clock clock;

    @Override
    @Transactional
    public void delete(DeleteBoardCommand command) {
        var board =
                boardFindPort
                        .findById(command.boardId())
                        .orElseThrow(() -> new IllegalArgumentException("Board를 찾을 수 없습니다."));

        var now = clock.instant();
        board.delete(now);

        boardSavePort.save(board);
    }
}
