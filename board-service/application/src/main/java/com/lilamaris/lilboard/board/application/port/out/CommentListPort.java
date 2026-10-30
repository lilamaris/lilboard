package com.lilamaris.lilboard.board.application.port.out;

import com.lilamaris.lilboard.board.domain.Comment;
import com.lilamaris.lilboard.kernel.pagination.CursorPage;
import com.lilamaris.lilboard.kernel.pagination.CursorPageRequest;
import java.util.UUID;

public interface CommentListPort {
    CursorPage<Comment, UUID> findVisibleByPostId(UUID postId, CursorPageRequest<UUID> pageRequest);
}
