package com.lilamaris.lilboard.board.application.port.in;

import com.lilamaris.lilboard.board.application.port.in.query.FindCommentQuery;
import com.lilamaris.lilboard.board.application.port.in.result.FindCommentResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface FindCommentUseCase {
    FindCommentResult find(@NotNull @Valid FindCommentQuery query);
}
