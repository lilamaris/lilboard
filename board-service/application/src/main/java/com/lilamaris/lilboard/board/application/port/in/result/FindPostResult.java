package com.lilamaris.lilboard.board.application.port.in.result;

import com.lilamaris.lilboard.board.domain.Post;
import java.time.Instant;
import java.util.UUID;

public record FindPostResult(
        UUID id, UUID boardId, String title, String content, Instant createdAt, Instant updatedAt) {
    public static FindPostResult from(Post post) {
        return new FindPostResult(
                post.getId(),
                post.getBoardId(),
                post.getTitle(),
                post.getContent(),
                post.getCreatedAt(),
                post.getUpdatedAt());
    }
}
