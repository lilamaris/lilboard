package com.lilamaris.lilboard.board.application.port.in;

import com.lilamaris.lilboard.board.application.port.in.command.UpdatePostCommand;
import com.lilamaris.lilboard.board.application.port.in.result.UpdatePostResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface UpdatePostUseCase {
    UpdatePostResult update(@NotNull @Valid UpdatePostCommand command);
}
