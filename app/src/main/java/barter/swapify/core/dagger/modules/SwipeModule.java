package barter.swapify.core.dagger.modules;

import javax.inject.Singleton;

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
    static String getStrng() {
        return "John deniel dela pena";
    }
}
