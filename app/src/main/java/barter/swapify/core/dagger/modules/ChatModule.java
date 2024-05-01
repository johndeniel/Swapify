package barter.swapify.core.dagger.modules;

import android.content.Context;

import javax.inject.Singleton;

import barter.swapify.core.network.ConnectionCheckerImpl;
import barter.swapify.features.chatroom.data.datasource.ChatRemoteDataSource;
import barter.swapify.features.chatroom.data.datasource.ChatRemoteDataSourceImpl;
import barter.swapify.features.chatroom.data.repositories.ChatRepositoryImpl;
import barter.swapify.features.chatroom.domain.repository.ChatRepository;
import barter.swapify.features.chatroom.presentation.pages.ChatRoomPage;
import barter.swapify.features.explore.data.datasource.ExploreRemoteDataSource;
import barter.swapify.features.explore.data.datasource.ExploreRemoteDataSourceImpl;
import barter.swapify.features.explore.data.repositories.ExploreRepositoryImpl;
import barter.swapify.features.explore.domain.repository.ExploreRepository;
import barter.swapify.features.explore.presentation.pages.ExplorePage;
import dagger.Module;
import dagger.Provides;
import dagger.android.ContributesAndroidInjector;

@Module
public abstract  class ChatModule {

    @ContributesAndroidInjector
    abstract ChatRoomPage contributeChatRoomPage();

    @Provides
    @Singleton
    static ChatRepository provideChatRepository(Context context) {
        return new ChatRepositoryImpl(new ConnectionCheckerImpl(context), provideChatRemoteDataSource());
    }

    @Provides
    @Singleton
    static ChatRemoteDataSource provideChatRemoteDataSource() {
        return new ChatRemoteDataSourceImpl();
    }

}
