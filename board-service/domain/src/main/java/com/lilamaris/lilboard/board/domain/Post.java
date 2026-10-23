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
@Table(name = "post")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post {
    private static final int MAX_TITLE_LENGTH = 300;
    private static final int MAX_CONTENT_LENGTH = 50000;

    @Id private UUID id;

    @Column(name = "board_id", nullable = false)
    private UUID boardId;

    @Column(name = "title", nullable = false, length = MAX_TITLE_LENGTH)
    private String title;

    @Column(name = "content", nullable = false, length = MAX_CONTENT_LENGTH)
    private String content;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    private Post(
            UUID id,
            UUID boardId,
            String title,
            String content,
            Instant createdAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id);
        this.boardId = Objects.requireNonNull(boardId);
        this.title = StringPrecondition.requireMaxLen(title, MAX_TITLE_LENGTH);
        this.content = StringPrecondition.requireMaxLen(content, MAX_CONTENT_LENGTH);
        this.createdAt = Objects.requireNonNull(createdAt);

        if (updatedAt != null) {
            this.updatedAt = TimePrecondition.requireAfterOrEqual(updatedAt, createdAt);
        }
    }

    public static Post of(UUID id, UUID boardId, String title, String content, Instant createdAt) {
        return new Post(id, boardId, title, content, createdAt, null);
    }

    public void updateTitle(String title, Instant updatedAt) {
        String validatedTitle = StringPrecondition.requireMaxLen(title, MAX_TITLE_LENGTH);
        update(updatedAt);
        this.title = validatedTitle;
    }

    public void updateContent(String content, Instant updatedAt) {
        String validatedContent = StringPrecondition.requireMaxLen(content, MAX_CONTENT_LENGTH);
        update(updatedAt);
        this.content = validatedContent;
    }

    private void update(Instant updatedAt) {
        this.updatedAt = TimePrecondition.requireAfterOrEqual(updatedAt, createdAt);
    }
}
