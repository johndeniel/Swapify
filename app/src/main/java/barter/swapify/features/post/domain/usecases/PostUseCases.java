package barter.swapify.features.post.domain.usecases;

import java.util.concurrent.CompletableFuture;

import barter.swapify.core.errors.Failure;
import barter.swapify.core.typedef.Either;
import barter.swapify.core.usecase.UseCase;
import barter.swapify.features.post.domain.entity.PostEntity;
import barter.swapify.features.post.domain.repository.PostRepository;

public class PostUseCases implements UseCase<Boolean, PostEntity> {

    private final PostRepository postRepository;

    public PostUseCases(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @Override
    public CompletableFuture<Either<Failure, Boolean>> invoke(PostEntity postEntity) {
        return postRepository.post(postEntity);
    }
}
