package com.lilamaris.lilboard.board.application.service;

import com.lilamaris.lilboard.board.application.port.in.UpdatePostUseCase;
import com.lilamaris.lilboard.board.application.port.in.command.UpdatePostCommand;
import com.lilamaris.lilboard.board.application.port.in.result.UpdatePostResult;
import com.lilamaris.lilboard.board.application.port.out.PostFindPort;
import com.lilamaris.lilboard.board.application.port.out.PostSavePort;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Validated
@Service
@RequiredArgsConstructor
public class UpdatePostService implements UpdatePostUseCase {
    private final PostSavePort postSavePort;
    private final PostFindPort postFindPort;
    private final Clock clock;

    @Override
    @Transactional
    public UpdatePostResult update(UpdatePostCommand command) {
        var post =
                postFindPort
                        .findById(command.postId())
                        .orElseThrow(() -> new IllegalArgumentException("Post를 찾을 수 없습니다."));

        var now = clock.instant();
        post.updateTitle(command.title(), now);
        post.updateContent(command.content(), now);

        var saved = postSavePort.save(post);

        return UpdatePostResult.from(saved);
    }
}
