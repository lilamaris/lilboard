package com.lilamaris.lilboard.board.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdatePostRequest(
        @NotBlank @Size(max = 300) String title, @NotBlank @Size(max = 50000) String content) {}
