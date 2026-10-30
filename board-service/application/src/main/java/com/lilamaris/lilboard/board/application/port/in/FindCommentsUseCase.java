package com.lilamaris.lilboard.board.application.port.in;

import com.lilamaris.lilboard.board.application.port.in.query.FindCommentsQuery;
import com.lilamaris.lilboard.board.application.port.in.result.FindCommentResult;
import com.lilamaris.lilboard.kernel.pagination.CursorPage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public interface FindCommentsUseCase {
    CursorPage<FindCommentResult, UUID> find(@NotNull @Valid FindCommentsQuery query);
}
