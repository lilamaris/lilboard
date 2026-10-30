package com.lilamaris.lilboard.board.application.service;

import com.lilamaris.lilboard.board.application.port.in.FindCommentUseCase;
import com.lilamaris.lilboard.board.application.port.in.query.FindCommentQuery;
import com.lilamaris.lilboard.board.application.port.in.result.FindCommentResult;
import com.lilamaris.lilboard.board.application.port.out.CommentFindPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Validated
@Service
@RequiredArgsConstructor
public class FindCommentService implements FindCommentUseCase {
    private final CommentFindPort commentFindPort;

    @Override
    @Transactional(readOnly = true)
    public FindCommentResult find(FindCommentQuery query) {
        var comment =
                commentFindPort
                        .findVisibleById(query.commentId())
                        .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));
        return FindCommentResult.from(comment);
    }
}
