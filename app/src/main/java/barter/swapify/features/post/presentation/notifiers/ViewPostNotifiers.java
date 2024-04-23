package barter.swapify.features.post.presentation.notifiers;

import java.util.List;

import barter.swapify.core.errors.Failure;
import barter.swapify.core.typedef.Either;
import barter.swapify.features.post.domain.entity.PostEntity;
import barter.swapify.features.post.domain.usecases.FetchUseCases;
import io.reactivex.rxjava3.core.Single;


public class ViewPostNotifiers {
    private final String uid;

    private final FetchUseCases fetchUseCases;

    public ViewPostNotifiers(String uid, FetchUseCases fetchUseCases) {
        this.uid = uid;
        this.fetchUseCases = fetchUseCases;
    }

    public Single<Either<Failure, List<PostEntity>>> fetch() {
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
