package barter.swapify.features.story.data.datasource;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import barter.swapify.core.errors.Failure;
import barter.swapify.core.typedef.Either;
import barter.swapify.features.story.data.model.StoryModel;

public interface StoryRemoteDataSource {
    CompletableFuture<Either<Failure, List<StoryModel>>> fetch();
}
