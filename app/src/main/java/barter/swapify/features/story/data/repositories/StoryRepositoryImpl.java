package barter.swapify.features.story.data.repositories;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import barter.swapify.core.constants.Constants;
import barter.swapify.core.errors.Failure;
import barter.swapify.core.network.ConnectionChecker;
import barter.swapify.core.typedef.Either;
import barter.swapify.features.story.data.datasource.StoryRemoteDataSource;
import barter.swapify.features.story.data.model.StoryModel;
import barter.swapify.features.story.domain.entity.StoryEntity;
import barter.swapify.features.story.domain.repository.StoryRepository;

public class StoryRepositoryImpl implements StoryRepository {
    private final ConnectionChecker connectionChecker;
    private final StoryRemoteDataSource storyRemoteDataSource;

    public StoryRepositoryImpl(ConnectionChecker connectionChecker, StoryRemoteDataSource storyRemoteDataSource) {
        this.connectionChecker = connectionChecker;
        this.storyRemoteDataSource = storyRemoteDataSource;
    }

    @Override
    public CompletableFuture<Either<Failure, List<StoryEntity>>> fetch() {
        return connectionChecker.isConnected()
                .thenCompose(isConnected -> {
                    if (isConnected) {
                        return storyRemoteDataSource.fetch().thenApply(either -> either.map(StoryModel::toStoryEntity));
                    } else {
                        return CompletableFuture.completedFuture(Either.left(new Failure(Constants.noConnectionErrorMessage)));
                    }
                });
    }
}
