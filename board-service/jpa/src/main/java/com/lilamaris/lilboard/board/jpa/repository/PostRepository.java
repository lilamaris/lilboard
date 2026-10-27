package com.lilamaris.lilboard.board.jpa.repository;

import com.lilamaris.lilboard.board.domain.Post;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, UUID> {
    @Query(
            "select p from Post p where p.boardId in (select b.id from Board b where b.deletedAt is null) and p.id = :id")
    Optional<Post> findVisibleById(@Param("id") UUID id);

    @Query(
            "select p from Post p where p.boardId in (select b.id from Board b where b.deletedAt is null)")
    Page<Post> findVisible(Pageable pageable);
}
