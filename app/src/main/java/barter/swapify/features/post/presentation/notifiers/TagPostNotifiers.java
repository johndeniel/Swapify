package barter.swapify.features.post.presentation.notifiers;

import java.util.List;

import barter.swapify.core.errors.Failure;
import barter.swapify.core.typedef.Either;
import barter.swapify.core.usecase.NoParams;
import barter.swapify.features.post.domain.entity.PostEntity;
import barter.swapify.features.post.domain.usecases.TagUseCases;
import io.reactivex.rxjava3.core.Single;

public class TagPostNotifiers {
    private final TagUseCases tagUseCases;

    public TagPostNotifiers(TagUseCases tagUseCases) {
        this.tagUseCases = tagUseCases;
    }

    public Single<Either<Failure, List<PostEntity>>> tag() {
        return Single.create(emitter -> tagUseCases.invoke(new NoParams())
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
