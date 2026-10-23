package com.lilamaris.lilboard.board.application.port.in.query;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record FindPostQuery(@NotNull UUID postId) {}
