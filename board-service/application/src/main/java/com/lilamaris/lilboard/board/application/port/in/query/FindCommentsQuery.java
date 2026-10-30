package com.lilamaris.lilboard.board.application.port.in.query;

import com.lilamaris.lilboard.kernel.pagination.CursorPageRequest;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record FindCommentsQuery(
        @NotNull UUID postId, @NotNull CursorPageRequest<UUID> pageRequest) {}
