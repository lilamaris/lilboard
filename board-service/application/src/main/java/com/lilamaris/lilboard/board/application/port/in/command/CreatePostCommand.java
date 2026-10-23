package com.lilamaris.lilboard.board.application.port.in.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreatePostCommand(
        @NotNull UUID boardId,
        @NotBlank @Size(max = 300) String title,
        @NotBlank @Size(max = 50000) String content) {}
