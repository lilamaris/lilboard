package com.lilamaris.lilboard.board.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateBoardRequest(
        @NotBlank @Size(max = 50) String name, @NotBlank @Size(max = 300) String description) {}
