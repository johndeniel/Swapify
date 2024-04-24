package barter.swapify.features.story.domain.repository;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import barter.swapify.core.errors.Failure;
import barter.swapify.core.typedef.Either;
import barter.swapify.features.story.domain.entity.StoryEntity;

public interface StoryRepository {
    CompletableFuture<Either<Failure, List<StoryEntity>>> fetch();
}
