package com.lilamaris.lilboard.board.application.port.in.result;

import com.lilamaris.lilboard.board.domain.Comment;
import java.time.Instant;
import java.util.UUID;

public record CreateCommentResult(
        UUID id, UUID postId, String content, Instant createdAt, Instant updatedAt) {
    public static CreateCommentResult from(Comment comment) {
        return new CreateCommentResult(
                comment.getId(),
                comment.getPostId(),
                comment.getContent(),
                comment.getCreatedAt(),
                comment.getUpdatedAt());
    }
}
