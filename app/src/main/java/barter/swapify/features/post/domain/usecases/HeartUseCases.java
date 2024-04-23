package barter.swapify.features.post.domain.usecases;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import barter.swapify.core.errors.Failure;
import barter.swapify.core.typedef.Either;
import barter.swapify.core.usecase.NoParams;
import barter.swapify.core.usecase.UseCase;
import barter.swapify.features.post.domain.entity.PostEntity;
import barter.swapify.features.post.domain.repository.PostRepository;

public class HeartUseCases implements UseCase<List<PostEntity>, NoParams> {
    private  final PostRepository postRepository;

    public HeartUseCases(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @Override
    public CompletableFuture<Either<Failure, List<PostEntity>>> invoke(NoParams noParams) {
        return postRepository.heart();
    }
}
