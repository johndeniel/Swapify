package barter.swapify.features.explore.domain.usecases;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import barter.swapify.core.errors.Failure;
import barter.swapify.core.typedef.Either;
import barter.swapify.core.usecase.NoParams;
import barter.swapify.core.usecase.UseCase;
import barter.swapify.features.explore.domain.repository.ExploreRepository;

public class BannerUseCases implements UseCase<List<String>, NoParams> {
    private final ExploreRepository exploreRepository;

    public BannerUseCases(ExploreRepository exploreRepository) {
        this.exploreRepository = exploreRepository;
    }

    @Override
    public CompletableFuture<Either<Failure, List<String>>> invoke(NoParams noParams) {
        return exploreRepository.banner();
    }
}
