package com.lilamaris.lilboard.board.application.port.in;

import com.lilamaris.lilboard.board.application.port.in.query.FindBoardQuery;
import com.lilamaris.lilboard.board.application.port.in.result.FindBoardResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface FindBoardUseCase {
    FindBoardResult find(@NotNull @Valid FindBoardQuery query);
}
