package com.lilamaris.lilboard.board.jpa;

import com.lilamaris.lilboard.board.application.port.out.CommentFindPort;
import com.lilamaris.lilboard.board.application.port.out.CommentListPort;
import com.lilamaris.lilboard.board.application.port.out.CommentSavePort;
import com.lilamaris.lilboard.board.domain.Comment;
import com.lilamaris.lilboard.board.jpa.repository.CommentRepository;
import com.lilamaris.lilboard.kernel.pagination.Cursor;
import com.lilamaris.lilboard.kernel.pagination.CursorPage;
import com.lilamaris.lilboard.kernel.pagination.CursorPageRequest;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommentPortJpaAdapter implements CommentSavePort, CommentFindPort, CommentListPort {
    private final CommentRepository repository;

    @Override
    public Comment save(Comment comment) {
        return repository.save(comment);
    }

    @Override
    public Optional<Comment> findVisibleById(UUID id) {
        return repository.findByDeletedAtIsNullAndId(id);
    }

    @Override
    public CursorPage<Comment, UUID> findVisibleByPostId(
            UUID postId, CursorPageRequest<UUID> pageRequest) {
        var cursor = pageRequest.cursor();
        var pageable =
                PageRequest.of(
                        0, pageRequest.size() + 1, Sort.by(Sort.Direction.DESC, "createdAt", "id"));

        Instant cursorCreatedAt = cursor == null ? null : cursor.createdAt();
        UUID cursorId = cursor == null ? null : cursor.id();

        var fetched = repository.findVisibleByPostId(postId, cursorCreatedAt, cursorId, pageable);

        return CursorPage.from(
                fetched,
                pageRequest.size(),
                comment -> Cursor.of(comment.getCreatedAt(), comment.getId()));
    }
}
