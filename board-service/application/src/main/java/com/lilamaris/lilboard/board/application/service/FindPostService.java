package com.lilamaris.lilboard.board.application.service;

import com.lilamaris.lilboard.board.application.port.in.FindPostUseCase;
import com.lilamaris.lilboard.board.application.port.in.query.FindPostQuery;
import com.lilamaris.lilboard.board.application.port.in.result.FindPostResult;
import com.lilamaris.lilboard.board.application.port.out.PostFindPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Validated
@Service
@RequiredArgsConstructor
public class FindPostService implements FindPostUseCase {
    private final PostFindPort postFindPort;

    @Override
    @Transactional(readOnly = true)
    public FindPostResult find(FindPostQuery query) {
        var post =
                postFindPort
                        .findById(query.postId())
                        .orElseThrow(() -> new IllegalArgumentException("Post를 찾을 수 없습니다."));
        return FindPostResult.from(post);
    }
}
