package com.lilamaris.lilboard.board.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCommentRequest(@NotBlank @Size(max = 5000) String content) {}
