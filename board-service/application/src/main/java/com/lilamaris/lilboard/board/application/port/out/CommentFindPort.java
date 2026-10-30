package com.lilamaris.lilboard.board.application.port.out;

import com.lilamaris.lilboard.board.domain.Comment;
import java.util.Optional;
import java.util.UUID;

public interface CommentFindPort {
    Optional<Comment> findVisibleById(UUID id);
}
