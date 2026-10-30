package com.lilamaris.lilboard.board.jpa.repository;

import com.lilamaris.lilboard.board.domain.Comment;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, UUID> {
    Optional<Comment> findByDeletedAtIsNullAndId(UUID id);

    @Query(
            """
            select c from Comment c
            where c.deletedAt is null
              and c.postId = :postId
              and (:cursorCreatedAt is null or c.createdAt < :cursorCreatedAt
                   or (c.createdAt = :cursorCreatedAt and c.id < :cursorId))
            order by c.createdAt desc, c.id desc
            """)
    List<Comment> findVisibleByPostId(
            @Param("postId") UUID postId,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorId") UUID cursorId,
            Pageable pageable);
}
