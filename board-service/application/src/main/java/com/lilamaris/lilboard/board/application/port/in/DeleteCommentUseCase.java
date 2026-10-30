package com.lilamaris.lilboard.board.application.port.in;

import com.lilamaris.lilboard.board.application.port.in.command.DeleteCommentCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface DeleteCommentUseCase {
    void delete(@NotNull @Valid DeleteCommentCommand command);
}
