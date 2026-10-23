package com.lilamaris.lilboard.board.application.service;

import com.lilamaris.lilboard.board.application.port.in.CreatePostUseCase;
import com.lilamaris.lilboard.board.application.port.in.command.CreatePostCommand;
import com.lilamaris.lilboard.board.application.port.in.result.CreatePostResult;
import com.lilamaris.lilboard.board.application.port.out.PostSavePort;
import com.lilamaris.lilboard.board.application.port.out.UuidGenerator;
import com.lilamaris.lilboard.board.domain.Post;
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
public class CreatePostService implements CreatePostUseCase {
    private final PostSavePort postSavePort;
    private final UuidGenerator uuidGenerator;
    private final Clock clock;

    @Override
    @Transactional
    public CreatePostResult create(CreatePostCommand command) {
        UUID id = uuidGenerator.generate();
        Instant createdAt = clock.instant();

        Post post = Post.of(id, command.boardId(), command.title(), command.content(), createdAt);
        Post saved = postSavePort.save(post);

        return CreatePostResult.from(saved);
    }
}
