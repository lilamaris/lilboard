package com.lilamaris.lilboard.board.application.port.in;

import com.lilamaris.lilboard.board.application.port.in.command.UpdateBoardCommand;
import com.lilamaris.lilboard.board.application.port.in.result.UpdateBoardResult;
import jakarta.validation.Valid;

public interface UpdateBoardUseCase {
    UpdateBoardResult update(@Valid UpdateBoardCommand command);
}
