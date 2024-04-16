package barter.swapify.features.swipe.presentation.notifiers;

import java.util.List;

import barter.swapify.core.errors.Failure;
import barter.swapify.core.typedef.Either;
import barter.swapify.features.swipe.domain.entity.SwipeEntity;
import barter.swapify.features.swipe.domain.usecases.FetchUseCases;
import io.reactivex.rxjava3.core.Single;

public class SwipeNotifiers {

    private final String uid;

    private final FetchUseCases fetchUseCases;

    public SwipeNotifiers(String uid, FetchUseCases fetchUseCases) {
        this.uid = uid;
        this.fetchUseCases = fetchUseCases;
    }

    public Single<Either<Failure, List<SwipeEntity>>> fetch() {
        return Single.create(emitter -> fetchUseCases.invoke(uid)
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
