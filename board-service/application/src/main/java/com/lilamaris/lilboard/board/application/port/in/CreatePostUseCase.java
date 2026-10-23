package com.lilamaris.lilboard.board.application.port.in;

import com.lilamaris.lilboard.board.application.port.in.command.CreatePostCommand;
import com.lilamaris.lilboard.board.application.port.in.result.CreatePostResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface CreatePostUseCase {
    CreatePostResult create(@NotNull @Valid CreatePostCommand command);
}
