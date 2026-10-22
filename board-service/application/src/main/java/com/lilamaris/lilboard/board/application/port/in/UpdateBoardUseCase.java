package com.lilamaris.lilboard.board.application.port.in;

import com.lilamaris.lilboard.board.application.port.in.command.UpdateBoardCommand;
import com.lilamaris.lilboard.board.application.port.in.result.UpdateBoardResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface UpdateBoardUseCase {
    UpdateBoardResult update(@NotNull @Valid UpdateBoardCommand command);
}
