package barter.swapify.core.dagger.modules;

import android.content.Context;

import javax.inject.Singleton;

import barter.swapify.core.network.ConnectionCheckerImpl;
import barter.swapify.features.explore.data.datasource.ExploreRemoteDataSource;
import barter.swapify.features.explore.data.datasource.ExploreRemoteDataSourceImpl;
import barter.swapify.features.explore.data.repositories.ExploreRepositoryImpl;
import barter.swapify.features.explore.domain.repository.ExploreRepository;
import barter.swapify.features.explore.presentation.pages.ExplorePage;
import barter.swapify.features.story.data.datasource.StoryRemoteDataSource;
import barter.swapify.features.story.data.datasource.StoryRemoteDataSourceImpl;
import barter.swapify.features.story.data.repositories.StoryRepositoryImpl;
import barter.swapify.features.story.domain.repository.StoryRepository;
import barter.swapify.features.story.presentation.pages.StoryPage;
import dagger.Module;
import dagger.Provides;
import dagger.android.ContributesAndroidInjector;

@Module
public abstract class StoryModule {

    @ContributesAndroidInjector
    abstract StoryPage contributeStoryPage();

    @Provides
    @Singleton
    static StoryRepository provideStoryRepository(Context context) {
        return new StoryRepositoryImpl(new ConnectionCheckerImpl(context), provideStoryRemoteDataSource());
    }

    @Provides
    @Singleton
    static StoryRemoteDataSource provideStoryRemoteDataSource() {
        return new StoryRemoteDataSourceImpl();
    }
}
