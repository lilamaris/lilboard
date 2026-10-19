package com.lilamaris.lilboard.board.jpa.repository;

import com.lilamaris.lilboard.board.domain.Board;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardRepository extends JpaRepository<Board, UUID> {}
