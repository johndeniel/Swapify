package barter.swapify.core.dagger.modules;

import android.content.Context;

import javax.inject.Singleton;

import barter.swapify.core.network.ConnectionCheckerImpl;
import barter.swapify.features.messenger.data.datasource.MessengerRemoteDataSource;
import barter.swapify.features.messenger.data.datasource.MessengerRemoteDataSourceImpl;
import barter.swapify.features.messenger.data.repositories.MessengerRepositoryImpl;
import barter.swapify.features.messenger.domain.repository.MessengerRepository;
import barter.swapify.features.messenger.presentation.pages.MessengerPage;
import barter.swapify.features.messenger.presentation.pages.RecentChatPage;
import dagger.Module;
import dagger.Provides;
import dagger.android.ContributesAndroidInjector;

@Module
public abstract class MessengerModule {

    @ContributesAndroidInjector
    abstract MessengerPage contributeMessengerPage();

    @ContributesAndroidInjector
    abstract RecentChatPage contributeRecentChatPage();


    @Provides
    @Singleton
    static MessengerRepository provideMessengerRepository(Context context) {
        return new MessengerRepositoryImpl(new ConnectionCheckerImpl(context), provideMessengerRemoteDataSource());
    }

    @Provides
    @Singleton
    static MessengerRemoteDataSource provideMessengerRemoteDataSource() {
        return new MessengerRemoteDataSourceImpl();
    }

}
