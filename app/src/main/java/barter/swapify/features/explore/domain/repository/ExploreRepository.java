package barter.swapify.features.explore.domain.repository;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import barter.swapify.core.errors.Failure;
import barter.swapify.core.typedef.Either;
public interface ExploreRepository {
    CompletableFuture<Either<Failure, List<String>>> banner();
}
