package com.lilamaris.lilboard.board.application.port.out;

import com.lilamaris.lilboard.board.domain.Post;
import java.util.Optional;
import java.util.UUID;

public interface PostFindPort {
    Optional<Post> findById(UUID id);
}
