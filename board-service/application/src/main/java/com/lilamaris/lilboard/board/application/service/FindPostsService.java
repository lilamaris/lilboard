package com.lilamaris.lilboard.board.application.service;

import com.lilamaris.lilboard.board.application.port.in.FindPostsUseCase;
import com.lilamaris.lilboard.board.application.port.in.query.FindPostsQuery;
import com.lilamaris.lilboard.board.application.port.in.result.FindPostResult;
import com.lilamaris.lilboard.board.application.port.out.PostListPort;
import com.lilamaris.lilboard.kernel.pagination.OffsetPage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Validated
@Service
@RequiredArgsConstructor
public class FindPostsService implements FindPostsUseCase {
    private final PostListPort postListPort;

    @Override
    @Transactional(readOnly = true)
    public OffsetPage<FindPostResult> find(FindPostsQuery query) {
        var page = postListPort.findAllVisible(query.pageRequest());
        return OffsetPage.of(
                page.items().stream().map(FindPostResult::from).toList(),
                page.page(),
                page.size(),
                page.totalElements());
    }
}
