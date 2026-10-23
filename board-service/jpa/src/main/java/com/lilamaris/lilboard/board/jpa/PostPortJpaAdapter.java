package com.lilamaris.lilboard.board.jpa;

import com.lilamaris.lilboard.board.application.port.out.PostFindPort;
import com.lilamaris.lilboard.board.application.port.out.PostSavePort;
import com.lilamaris.lilboard.board.domain.Post;
import com.lilamaris.lilboard.board.jpa.repository.PostRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PostPortJpaAdapter implements PostSavePort, PostFindPort {
    private final PostRepository repository;

    @Override
    public Post save(Post post) {
        return repository.save(post);
    }

    @Override
    public Optional<Post> findById(UUID id) {
        return repository.findById(id);
    }
}
