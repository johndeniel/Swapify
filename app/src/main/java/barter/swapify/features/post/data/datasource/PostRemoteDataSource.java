package barter.swapify.features.post.data.datasource;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import barter.swapify.core.errors.Failure;
import barter.swapify.core.typedef.Either;
import barter.swapify.features.post.data.model.PostModel;
import barter.swapify.features.post.domain.entity.PostEntity;

public interface PostRemoteDataSource {
    CompletableFuture<Either<Failure, List<PostModel>>> fetch(String uid);
    CompletableFuture<Either<Failure, List<PostModel>>> heart();
    CompletableFuture<Either<Failure, List<PostModel>>> tag();
}
