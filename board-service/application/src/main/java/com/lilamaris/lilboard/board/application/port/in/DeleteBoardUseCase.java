package com.lilamaris.lilboard.board.application.port.in;

import com.lilamaris.lilboard.board.application.port.in.command.DeleteBoardCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface DeleteBoardUseCase {
    void delete(@NotNull @Valid DeleteBoardCommand command);
}
