package com.lilamaris.lilboard.board.application.port.in;

import com.lilamaris.lilboard.board.application.port.in.command.CreateCommentCommand;
import com.lilamaris.lilboard.board.application.port.in.result.CreateCommentResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface CreateCommentUseCase {
    CreateCommentResult create(@NotNull @Valid CreateCommentCommand command);
}
