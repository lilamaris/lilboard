package com.lilamaris.lilboard.board.application.port.in.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record UpdateBoardCommand(
        @NotNull UUID boardId,
        @NotBlank @Size(max = 50) String name,
        @NotBlank @Size(max = 300) String description) {}
