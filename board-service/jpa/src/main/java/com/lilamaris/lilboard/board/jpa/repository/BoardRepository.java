package com.lilamaris.lilboard.board.jpa.repository;

import com.lilamaris.lilboard.board.domain.Board;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardRepository extends JpaRepository<Board, UUID> {
    Optional<Board> findByDeletedAtIsNullAndId(UUID id);

    Page<Board> findByDeletedAtIsNull(Pageable pageable);
}
