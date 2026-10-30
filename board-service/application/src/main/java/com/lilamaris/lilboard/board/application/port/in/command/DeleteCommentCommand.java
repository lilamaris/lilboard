package com.lilamaris.lilboard.board.application.port.in.command;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record DeleteCommentCommand(@NotNull UUID commentId) {}
