package com.lilamaris.lilboard.board.jpa;

import com.lilamaris.lilboard.board.application.port.out.PostFindPort;
import com.lilamaris.lilboard.board.application.port.out.PostListPort;
import com.lilamaris.lilboard.board.application.port.out.PostSavePort;
import com.lilamaris.lilboard.board.domain.Post;
import com.lilamaris.lilboard.board.jpa.repository.PostRepository;
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
public class PostPortJpaAdapter implements PostSavePort, PostFindPort, PostListPort {
    private final PostRepository repository;

    @Override
    public Post save(Post post) {
        return repository.save(post);
    }

    @Override
    public Optional<Post> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public OffsetPage<Post> findAll(OffsetPageRequest pageRequest) {
        var pageable =
                PageRequest.of(
                        pageRequest.page(),
                        pageRequest.size(),
                        Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        var page = repository.findAll(pageable);
        return OffsetPage.of(
                page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements());
    }
}
