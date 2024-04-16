package barter.swapify.features.swipe.data.datasource;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import barter.swapify.core.errors.Failure;
import barter.swapify.core.typedef.Either;
import barter.swapify.features.swipe.data.model.SwipeModel;

public interface SwipeRemoteDataSource {
    CompletableFuture<Either<Failure, List<SwipeModel>>> fetch(String uid);
}
