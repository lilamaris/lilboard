package com.lilamaris.lilboard.board.application.service;

import com.lilamaris.lilboard.board.application.port.in.DeleteCommentUseCase;
import com.lilamaris.lilboard.board.application.port.in.command.DeleteCommentCommand;
import com.lilamaris.lilboard.board.application.port.out.CommentFindPort;
import com.lilamaris.lilboard.board.application.port.out.CommentSavePort;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Validated
@Service
@RequiredArgsConstructor
public class DeleteCommentService implements DeleteCommentUseCase {
    private final CommentSavePort commentSavePort;
    private final CommentFindPort commentFindPort;
    private final Clock clock;

    @Override
    @Transactional
    public void delete(DeleteCommentCommand command) {
        var comment =
                commentFindPort
                        .findVisibleById(command.commentId())
                        .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));

        var now = clock.instant();
        comment.delete(now);

        commentSavePort.save(comment);
    }
}
