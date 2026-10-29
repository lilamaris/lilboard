package com.lilamaris.lilboard.board.domain;

import com.lilamaris.lilboard.kernel.condition.StringPrecondition;
import com.lilamaris.lilboard.kernel.condition.TimePrecondition;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "comment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment {
    private static final int MAX_CONTENT_LENGTH = 5000;

    @Id private UUID id;

    @Column(name = "post_id", nullable = false)
    private UUID postId;

    @Column(name = "content", nullable = false, length = MAX_CONTENT_LENGTH)
    private String content;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    private Comment(
            UUID id,
            UUID postId,
            String content,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt) {
        this.id = Objects.requireNonNull(id);
        this.postId = Objects.requireNonNull(postId);
        this.content = StringPrecondition.requireMaxLen(content, MAX_CONTENT_LENGTH);
        this.createdAt = Objects.requireNonNull(createdAt);

        if (updatedAt != null)
            this.updatedAt = TimePrecondition.requireAfterOrEqual(updatedAt, createdAt);

        if (deletedAt != null) TimePrecondition.requireAfterOrEqual(deletedAt, createdAt);
    }

    public static Comment of(UUID id, UUID postId, String content, Instant createdAt) {
        return new Comment(id, postId, content, createdAt, null, null);
    }

    public void updateContent(String content, Instant updatedAt) {
        String validatedContent = StringPrecondition.requireMaxLen(content, MAX_CONTENT_LENGTH);
        this.updatedAt = TimePrecondition.requireAfterOrEqual(updatedAt, createdAt);
        this.content = validatedContent;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void delete(Instant deletedAt) {
        if (deletedAt == null) {
            throw new NullPointerException("deletedAt must not be null.");
        }
        if (this.deletedAt != null) {
            throw new IllegalStateException("already deleted.");
        }
        this.deletedAt = TimePrecondition.requireAfterOrEqual(deletedAt, createdAt);
    }
}
