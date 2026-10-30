package com.lilamaris.lilboard.board.application.service;

import com.lilamaris.lilboard.board.application.port.in.FindCommentsUseCase;
import com.lilamaris.lilboard.board.application.port.in.query.FindCommentsQuery;
import com.lilamaris.lilboard.board.application.port.in.result.FindCommentResult;
import com.lilamaris.lilboard.board.application.port.out.CommentListPort;
import com.lilamaris.lilboard.kernel.pagination.CursorPage;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Validated
@Service
@RequiredArgsConstructor
public class FindCommentsService implements FindCommentsUseCase {
    private final CommentListPort commentListPort;

    @Override
    @Transactional(readOnly = true)
    public CursorPage<FindCommentResult, UUID> find(FindCommentsQuery query) {
        var page = commentListPort.findVisibleByPostId(query.postId(), query.pageRequest());
        return CursorPage.of(
                page.items().stream().map(FindCommentResult::from).toList(), page.nextCursor());
    }
}
