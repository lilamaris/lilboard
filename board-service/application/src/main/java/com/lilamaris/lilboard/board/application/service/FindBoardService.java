package com.lilamaris.lilboard.board.application.service;

import com.lilamaris.lilboard.board.application.port.in.FindBoardUseCase;
import com.lilamaris.lilboard.board.application.port.in.query.FindBoardQuery;
import com.lilamaris.lilboard.board.application.port.in.result.FindBoardResult;
import com.lilamaris.lilboard.board.application.port.out.BoardFindPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Validated
@Service
@RequiredArgsConstructor
public class FindBoardService implements FindBoardUseCase {
    private final BoardFindPort boardFindPort;

    @Override
    @Transactional(readOnly = true)
    public FindBoardResult find(FindBoardQuery query) {
        var board =
                boardFindPort
                        .findById(query.boardId())
                        .orElseThrow(() -> new IllegalArgumentException("Board를 찾을 수 없습니다."));
        return FindBoardResult.from(board);
    }
}
