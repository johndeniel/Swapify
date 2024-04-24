package barter.swapify.features.story.presentation.notifiers;

import java.util.List;

import barter.swapify.core.errors.Failure;
import barter.swapify.core.typedef.Either;
import barter.swapify.core.usecase.NoParams;
import barter.swapify.features.story.domain.entity.StoryEntity;
import barter.swapify.features.story.domain.usecases.FetchUseCases;
import io.reactivex.rxjava3.core.Single;

public class StoryNotifiers {
    private final FetchUseCases fetchUseCases;

    public StoryNotifiers(FetchUseCases fetchUseCases) {
        this.fetchUseCases = fetchUseCases;
    }

    public Single<Either<Failure, List<StoryEntity>>> fetch() {
        return Single.create(emitter -> fetchUseCases.invoke(new NoParams())
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
