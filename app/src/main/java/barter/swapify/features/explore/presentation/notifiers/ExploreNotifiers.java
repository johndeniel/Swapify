package barter.swapify.features.explore.presentation.notifiers;

import java.util.List;

import barter.swapify.core.errors.Failure;
import barter.swapify.core.typedef.Either;
import barter.swapify.core.usecase.NoParams;
import barter.swapify.features.explore.domain.usecases.BannerUseCases;
import barter.swapify.features.swipe.domain.entity.SwipeEntity;
import io.reactivex.rxjava3.core.Single;

public class ExploreNotifiers {

    private final BannerUseCases bannerUseCases;

    public ExploreNotifiers(BannerUseCases bannerUseCases) {
        this.bannerUseCases = bannerUseCases;
    }

    public Single<Either<Failure, List<String>>> banner() {
        return Single.create(emitter -> bannerUseCases.invoke(new NoParams())
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
