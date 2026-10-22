package com.lilamaris.lilboard.board.application.port.in;

import com.lilamaris.lilboard.board.application.port.in.query.FindBoardsQuery;
import com.lilamaris.lilboard.board.application.port.in.result.FindBoardResult;
import com.lilamaris.lilboard.kernel.pagination.OffsetPage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface FindBoardsUseCase {
    OffsetPage<FindBoardResult> find(@NotNull @Valid FindBoardsQuery query);
}
