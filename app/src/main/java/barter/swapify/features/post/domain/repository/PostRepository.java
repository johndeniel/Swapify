package barter.swapify.features.post.domain.repository;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import barter.swapify.core.errors.Failure;
import barter.swapify.core.typedef.Either;
import barter.swapify.features.post.domain.entity.PostEntity;

public interface PostRepository {
    CompletableFuture<Either<Failure, List<PostEntity>>> fetch(String uid);
    CompletableFuture<Either<Failure, List<PostEntity>>> heart();
    CompletableFuture<Either<Failure, List<PostEntity>>> tag();
}
