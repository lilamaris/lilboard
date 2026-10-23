package com.lilamaris.lilboard.board.application.port.in;

import com.lilamaris.lilboard.board.application.port.in.query.FindPostQuery;
import com.lilamaris.lilboard.board.application.port.in.result.FindPostResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface FindPostUseCase {
    FindPostResult find(@NotNull @Valid FindPostQuery query);
}
