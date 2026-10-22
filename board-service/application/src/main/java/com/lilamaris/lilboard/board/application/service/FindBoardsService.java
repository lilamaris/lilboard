package com.lilamaris.lilboard.board.application.service;

import com.lilamaris.lilboard.board.application.port.in.FindBoardsUseCase;
import com.lilamaris.lilboard.board.application.port.in.query.FindBoardsQuery;
import com.lilamaris.lilboard.board.application.port.in.result.FindBoardResult;
import com.lilamaris.lilboard.board.application.port.out.BoardListPort;
import com.lilamaris.lilboard.kernel.pagination.OffsetPage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Validated
@Service
@RequiredArgsConstructor
public class FindBoardsService implements FindBoardsUseCase {
    private final BoardListPort boardListPort;

    @Override
    @Transactional(readOnly = true)
    public OffsetPage<FindBoardResult> find(FindBoardsQuery query) {
        var page = boardListPort.findAll(query.pageRequest());
        return OffsetPage.of(
                page.items().stream().map(FindBoardResult::from).toList(),
                page.page(),
                page.size(),
                page.totalElements());
    }
}
