package barter.swapify.features.post.domain.usecases;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import barter.swapify.core.errors.Failure;
import barter.swapify.core.typedef.Either;
import barter.swapify.core.usecase.UseCase;
import barter.swapify.features.post.domain.entity.PostEntity;
import barter.swapify.features.post.domain.repository.PostRepository;

public class FetchUseCases implements UseCase<List<PostEntity>, String> {
    private  final PostRepository postRepository;

    public FetchUseCases(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @Override
    public CompletableFuture<Either<Failure, List<PostEntity>>> invoke(String s) {
        return postRepository.fetch(s);
    }
}
