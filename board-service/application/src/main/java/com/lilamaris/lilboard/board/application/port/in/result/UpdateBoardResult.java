package com.lilamaris.lilboard.board.application.port.in.result;

import com.lilamaris.lilboard.board.domain.Board;
import java.time.Instant;
import java.util.UUID;

public record UpdateBoardResult(
        UUID id, String name, String description, Instant createdAt, Instant updatedAt) {
    public static UpdateBoardResult from(Board board) {
        return new UpdateBoardResult(
                board.getId(),
                board.getName(),
                board.getDescription(),
                board.getCreatedAt(),
                board.getUpdatedAt());
    }
}
