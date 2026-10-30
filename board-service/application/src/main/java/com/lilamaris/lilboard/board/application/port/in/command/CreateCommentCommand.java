package com.lilamaris.lilboard.board.application.port.in.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateCommentCommand(
        @NotNull UUID postId, @NotBlank @Size(max = 5000) String content) {}
