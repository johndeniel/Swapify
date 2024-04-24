package barter.swapify.features.story.domain.usecases;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import barter.swapify.core.errors.Failure;
import barter.swapify.core.typedef.Either;
import barter.swapify.core.usecase.NoParams;
import barter.swapify.core.usecase.UseCase;
import barter.swapify.features.story.domain.entity.StoryEntity;
import barter.swapify.features.story.domain.repository.StoryRepository;

public class FetchUseCases implements UseCase<List<StoryEntity>, NoParams> {
    private final StoryRepository storyRepository;

    public FetchUseCases(StoryRepository storyRepository) {
        this.storyRepository = storyRepository;
    }

    @Override
    public CompletableFuture<Either<Failure, List<StoryEntity>>> invoke(NoParams noParams) {
        return storyRepository.fetch();
    }
}
