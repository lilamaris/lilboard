package com.lilamaris.lilboard.board.application.port.in;

import com.lilamaris.lilboard.board.application.port.in.command.UpdateCommentCommand;
import com.lilamaris.lilboard.board.application.port.in.result.UpdateCommentResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface UpdateCommentUseCase {
    UpdateCommentResult update(@NotNull @Valid UpdateCommentCommand command);
}
