package barter.swapify.core.dagger.modules;

import android.content.Context;
import javax.inject.Singleton;

import barter.swapify.core.network.ConnectionCheckerImpl;
import barter.swapify.features.explore.data.datasource.ExploreRemoteDataSource;
import barter.swapify.features.explore.data.datasource.ExploreRemoteDataSourceImpl;
import barter.swapify.features.explore.data.repositories.ExploreRepositoryImpl;
import barter.swapify.features.explore.domain.repository.ExploreRepository;
import barter.swapify.features.explore.presentation.pages.ExplorePage;
import dagger.Module;
import dagger.Provides;
import dagger.android.ContributesAndroidInjector;

@Module
public abstract class ExploreModule {

    @ContributesAndroidInjector
    abstract ExplorePage contributeExplorePage();

    @Provides
    @Singleton
    static ExploreRepository provideExploreRepository(Context context) {
        return new ExploreRepositoryImpl(new ConnectionCheckerImpl(context), provideExploreRemoteDataSource());
    }

    @Provides
    @Singleton
    static ExploreRemoteDataSource provideExploreRemoteDataSource() {
        return new ExploreRemoteDataSourceImpl();
    }

}
