package barter.swapify.features.post.presentation.notifiers;

import java.util.List;

import barter.swapify.core.errors.Failure;
import barter.swapify.core.typedef.Either;
import barter.swapify.core.usecase.NoParams;
import barter.swapify.features.post.domain.entity.PostEntity;
import barter.swapify.features.post.domain.usecases.PostUseCases;
import io.reactivex.rxjava3.core.Single;

public class NewPostNotifiers {
    private final PostEntity post;
    private final PostUseCases postUseCases;

    public NewPostNotifiers(PostEntity post, PostUseCases postUseCases) {
        this.post = post;
        this.postUseCases = postUseCases;
    }

    public Single<Either<Failure, Boolean>> post() {
        return Single.create(emitter -> postUseCases.invoke(post)
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
