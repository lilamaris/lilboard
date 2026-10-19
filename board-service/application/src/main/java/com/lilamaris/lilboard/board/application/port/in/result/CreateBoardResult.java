package com.lilamaris.lilboard.board.application.port.in.result;

import com.lilamaris.lilboard.board.domain.Board;
import java.time.Instant;
import java.util.UUID;

public record CreateBoardResult(UUID id, String name, String description, Instant createdAt) {
    public static CreateBoardResult from(Board board) {
        return new CreateBoardResult(
                board.getId(), board.getName(), board.getDescription(), board.getCreatedAt());
    }
}
