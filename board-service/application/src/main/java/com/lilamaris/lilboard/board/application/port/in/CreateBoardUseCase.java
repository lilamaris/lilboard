package com.lilamaris.lilboard.board.application.port.in;

import com.lilamaris.lilboard.board.application.port.in.command.CreateBoardCommand;
import com.lilamaris.lilboard.board.application.port.in.result.CreateBoardResult;
import jakarta.validation.Valid;

public interface CreateBoardUseCase {
    CreateBoardResult create(@Valid CreateBoardCommand command);
}
