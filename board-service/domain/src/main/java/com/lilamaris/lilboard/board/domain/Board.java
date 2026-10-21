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
@Table(name = "board")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Board {
    private static final int MAX_NAME_LENGTH = 50;
    private static final int MAX_DESCRIPTION_LENGTH = 300;

    @Id private UUID id;

    @Column(name = "name", nullable = false, length = MAX_NAME_LENGTH)
    private String name;

    @Column(name = "description", nullable = false, length = MAX_DESCRIPTION_LENGTH)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    private Board(UUID id, String name, String description, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null.");
        this.name = StringPrecondition.requireMaxLen(name, MAX_NAME_LENGTH);
        this.description = StringPrecondition.requireMaxLen(description, MAX_DESCRIPTION_LENGTH);
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null.");

        if (updatedAt != null) TimePrecondition.requireAfterOrEqual(updatedAt, createdAt);
    }

    public static Board of(UUID id, String name, String description, Instant createdAt) {
        return new Board(id, name, description, createdAt, null);
    }

    public void updateName(String name, Instant updatedAt) {
        String validatedName = StringPrecondition.requireMaxLen(name, MAX_NAME_LENGTH);
        update(updatedAt);
        this.name = validatedName;
    }

    public void updateDescription(String description, Instant updatedAt) {
        String validatedDescription =
                StringPrecondition.requireMaxLen(description, MAX_DESCRIPTION_LENGTH);
        update(updatedAt);
        this.description = validatedDescription;
    }

    private void update(Instant updatedAt) {
        this.updatedAt = TimePrecondition.requireAfterOrEqual(updatedAt, createdAt);
    }
}
