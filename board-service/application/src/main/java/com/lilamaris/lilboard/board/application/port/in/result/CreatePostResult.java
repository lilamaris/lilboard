package com.lilamaris.lilboard.board.application.port.in.result;

import com.lilamaris.lilboard.board.domain.Post;
import java.time.Instant;
import java.util.UUID;

public record CreatePostResult(
        UUID id, UUID boardId, String title, String content, Instant createdAt) {
    public static CreatePostResult from(Post post) {
        return new CreatePostResult(
                post.getId(),
                post.getBoardId(),
                post.getTitle(),
                post.getContent(),
                post.getCreatedAt());
    }
}
