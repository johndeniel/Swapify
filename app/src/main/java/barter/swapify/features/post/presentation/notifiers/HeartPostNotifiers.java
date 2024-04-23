package barter.swapify.features.post.presentation.notifiers;

import java.util.List;

import barter.swapify.core.errors.Failure;
import barter.swapify.core.typedef.Either;
import barter.swapify.core.usecase.NoParams;
import barter.swapify.features.post.domain.entity.PostEntity;
import barter.swapify.features.post.domain.usecases.HeartUseCases;
import io.reactivex.rxjava3.core.Single;

public class HeartPostNotifiers {

    private final HeartUseCases heartUseCases;

    public HeartPostNotifiers(HeartUseCases heartUseCases) {
        this.heartUseCases = heartUseCases;
    }

    public Single<Either<Failure, List<PostEntity>>> heart() {
        return Single.create(emitter -> heartUseCases.invoke(new NoParams())
                .thenApplyAsync(result -> {
                    emitter.onSuccess(result);
                    return null;
                })
                .exceptionally(throwable -> {
                    emitter.onError(throwable);
                    return null;
                }));
    }
}
