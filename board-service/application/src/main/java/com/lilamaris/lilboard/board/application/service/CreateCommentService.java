package com.lilamaris.lilboard.board.application.service;

import com.lilamaris.lilboard.board.application.port.in.CreateCommentUseCase;
import com.lilamaris.lilboard.board.application.port.in.command.CreateCommentCommand;
import com.lilamaris.lilboard.board.application.port.in.result.CreateCommentResult;
import com.lilamaris.lilboard.board.application.port.out.CommentSavePort;
import com.lilamaris.lilboard.board.application.port.out.PostFindPort;
import com.lilamaris.lilboard.board.application.port.out.UuidGenerator;
import com.lilamaris.lilboard.board.domain.Comment;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Validated
@Service
@RequiredArgsConstructor
public class CreateCommentService implements CreateCommentUseCase {
    private final CommentSavePort commentSavePort;
    private final PostFindPort postFindPort;
    private final UuidGenerator uuidGenerator;
    private final Clock clock;

    @Override
    @Transactional
    public CreateCommentResult create(CreateCommentCommand command) {
        postFindPort
                .findVisibleById(command.postId())
                .orElseThrow(() -> new IllegalArgumentException("Post를 찾을 수 없습니다."));

        UUID id = uuidGenerator.generate();
        Instant createdAt = clock.instant();

        Comment comment = Comment.of(id, command.postId(), command.content(), createdAt);
        Comment saved = commentSavePort.save(comment);

        return CreateCommentResult.from(saved);
    }
}
