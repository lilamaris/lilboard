package com.lilamaris.lilboard.board.application.port.in;

import com.lilamaris.lilboard.board.application.port.in.query.FindPostsQuery;
import com.lilamaris.lilboard.board.application.port.in.result.FindPostResult;
import com.lilamaris.lilboard.kernel.pagination.OffsetPage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface FindPostsUseCase {
    OffsetPage<FindPostResult> find(@NotNull @Valid FindPostsQuery query);
}
