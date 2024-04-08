package barter.swapify.core.usecase;

import java.util.concurrent.CompletableFuture;

import barter.swapify.core.errors.Failure;
import barter.swapify.core.typedef.Either;

public interface UseCase<Type, Params> {
    public abstract CompletableFuture<Either<Failure, Type>> invoke(Params params);
}
