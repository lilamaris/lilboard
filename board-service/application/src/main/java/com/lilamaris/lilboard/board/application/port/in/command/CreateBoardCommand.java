package com.lilamaris.lilboard.board.application.port.in.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateBoardCommand(
        @NotBlank @Size(max = 50) String name, @NotBlank @Size(max = 300) String description) {}
