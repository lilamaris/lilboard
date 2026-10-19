package com.lilamaris.lilboard.board.application.port.in;

import com.lilamaris.lilboard.board.application.port.in.command.CreateBoardCommand;
import com.lilamaris.lilboard.board.application.port.in.result.CreateBoardResult;

public interface CreateBoardUseCase {
    CreateBoardResult create(CreateBoardCommand command);
}
