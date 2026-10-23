package com.lilamaris.lilboard.board.application.port.in.query;

import com.lilamaris.lilboard.kernel.pagination.OffsetPageRequest;
import jakarta.validation.constraints.NotNull;

public record FindPostsQuery(@NotNull OffsetPageRequest pageRequest) {}
