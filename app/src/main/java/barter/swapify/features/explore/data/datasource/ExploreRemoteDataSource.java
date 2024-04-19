package barter.swapify.features.explore.data.datasource;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import barter.swapify.core.errors.Failure;
import barter.swapify.core.typedef.Either;

public interface ExploreRemoteDataSource {
    CompletableFuture<Either<Failure, List<String>>> banner();
}
