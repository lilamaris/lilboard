package com.lilamaris.lilboard.board.jpa;

import com.lilamaris.lilboard.board.application.port.out.BoardFindPort;
import com.lilamaris.lilboard.board.application.port.out.BoardListPort;
import com.lilamaris.lilboard.board.application.port.out.BoardSavePort;
import com.lilamaris.lilboard.board.domain.Board;
import com.lilamaris.lilboard.board.jpa.repository.BoardRepository;
import com.lilamaris.lilboard.kernel.pagination.OffsetPage;
import com.lilamaris.lilboard.kernel.pagination.OffsetPageRequest;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BoardPortJpaAdapter implements BoardSavePort, BoardFindPort, BoardListPort {
    private final BoardRepository repository;

    @Override
    public Board save(Board board) {
        return repository.save(board);
    }

    @Override
    public Optional<Board> findById(UUID id) {
        return repository.findByDeletedAtIsNullAndId(id);
    }

    @Override
    public OffsetPage<Board> findAll(OffsetPageRequest pageRequest) {
        var pageable =
                PageRequest.of(
                        pageRequest.page(),
                        pageRequest.size(),
                        Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        var page = repository.findByDeletedAtIsNull(pageable);
        return OffsetPage.of(
                page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements());
    }
}
