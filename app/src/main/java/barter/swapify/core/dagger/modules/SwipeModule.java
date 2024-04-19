package barter.swapify.core.dagger.modules;

import static barter.swapify.core.constants.Constants.ITEMS_REFERENCE;

import android.content.Context;

import com.google.firebase.database.FirebaseDatabase;

import javax.inject.Singleton;

import barter.swapify.core.network.ConnectionCheckerImpl;
import barter.swapify.features.swipe.data.datasource.SwipeRemoteDataSource;
import barter.swapify.features.swipe.data.datasource.SwipeRemoteDataSourceImpl;
import barter.swapify.features.swipe.data.repositories.SwipeRepositoryImpl;
import barter.swapify.features.swipe.domain.repository.SwipeRepository;
import barter.swapify.features.swipe.presentation.pages.SwipePage;
import dagger.Module;
import dagger.Provides;
import dagger.android.ContributesAndroidInjector;

@Module
public abstract class SwipeModule {

    @ContributesAndroidInjector
    abstract SwipePage contributeSwipePage();

    @Provides
    @Singleton
    static SwipeRepository provideSwipeRepository(Context context) {
        return new SwipeRepositoryImpl(new ConnectionCheckerImpl(context), provideSwipeRemoteDataSource());
    }

    @Provides
    @Singleton
    static SwipeRemoteDataSource provideSwipeRemoteDataSource() {
        return new SwipeRemoteDataSourceImpl(FirebaseDatabase.getInstance().getReference(ITEMS_REFERENCE));
    }
}
