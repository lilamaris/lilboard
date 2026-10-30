package com.lilamaris.lilboard.board.application.service;

import com.lilamaris.lilboard.board.application.port.in.UpdateCommentUseCase;
import com.lilamaris.lilboard.board.application.port.in.command.UpdateCommentCommand;
import com.lilamaris.lilboard.board.application.port.in.result.UpdateCommentResult;
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
public class UpdateCommentService implements UpdateCommentUseCase {
    private final CommentSavePort commentSavePort;
    private final CommentFindPort commentFindPort;
    private final Clock clock;

    @Override
    @Transactional
    public UpdateCommentResult update(UpdateCommentCommand command) {
        var comment =
                commentFindPort
                        .findVisibleById(command.commentId())
                        .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));

        var now = clock.instant();
        comment.updateContent(command.content(), now);

        var saved = commentSavePort.save(comment);

        return UpdateCommentResult.from(saved);
    }
}
