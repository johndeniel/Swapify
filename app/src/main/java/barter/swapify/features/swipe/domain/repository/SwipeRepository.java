package barter.swapify.features.swipe.domain.repository;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import barter.swapify.core.errors.Failure;
import barter.swapify.core.typedef.Either;
import barter.swapify.features.swipe.domain.entity.SwipeEntity;

public interface SwipeRepository {
    CompletableFuture<Either<Failure, List<SwipeEntity>>> fetch(String uid);
}
