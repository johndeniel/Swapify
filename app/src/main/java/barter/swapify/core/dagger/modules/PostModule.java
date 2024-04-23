package barter.swapify.core.dagger.modules;

import static barter.swapify.core.constants.Constants.ITEMS_REFERENCE;

import android.content.Context;

import com.google.firebase.database.FirebaseDatabase;

import javax.inject.Singleton;

import barter.swapify.core.network.ConnectionCheckerImpl;
import barter.swapify.features.post.data.datasource.PostRemoteDataSource;
import barter.swapify.features.post.data.datasource.PostRemoteDataSourceImpl;
import barter.swapify.features.post.data.repositories.PostRepositoryImpl;
import barter.swapify.features.post.domain.repository.PostRepository;
import barter.swapify.features.post.presentation.pages.HeartPostPage;
import barter.swapify.features.post.presentation.pages.TagPostPage;
import barter.swapify.features.post.presentation.pages.ViewPostPage;
import dagger.Module;
import dagger.Provides;
import dagger.android.ContributesAndroidInjector;

@Module
public abstract class PostModule {

    @ContributesAndroidInjector
    abstract ViewPostPage contributeViewPostPage();

    @ContributesAndroidInjector
    abstract HeartPostPage contributeHeartPostPage();

    @ContributesAndroidInjector
    abstract TagPostPage contributeTagPostPage();

    @Provides
    @Singleton
    static PostRepository providePostRepository(Context context) {
        return new PostRepositoryImpl(new ConnectionCheckerImpl(context), providePostRemoteDataSource());
    }

    @Provides
    @Singleton
    static PostRemoteDataSource providePostRemoteDataSource() {
        return new PostRemoteDataSourceImpl(FirebaseDatabase.getInstance().getReference(ITEMS_REFERENCE));
    }
}
