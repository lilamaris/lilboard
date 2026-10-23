package com.lilamaris.lilboard.board.jpa.repository;

import com.lilamaris.lilboard.board.domain.Post;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, UUID> {}
