package com.lilamaris.lilboard.board.application.port.in.result;

import com.lilamaris.lilboard.board.domain.Board;
import java.time.Instant;
import java.util.UUID;

public record FindBoardResult(
        UUID id, String name, String description, Instant createdAt, Instant updatedAt) {
    public static FindBoardResult from(Board board) {
        return new FindBoardResult(
                board.getId(),
                board.getName(),
                board.getDescription(),
                board.getCreatedAt(),
                board.getUpdatedAt());
    }
}
