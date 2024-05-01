package barter.swapify.core.dagger;

import android.app.Application;
import android.content.Context;

import javax.inject.Singleton;

import barter.swapify.core.dagger.modules.AuthModule;
import barter.swapify.core.dagger.modules.ChatModule;
import barter.swapify.core.dagger.modules.ExploreModule;
import barter.swapify.core.dagger.modules.MessengerModule;
import barter.swapify.core.dagger.modules.PostModule;
import barter.swapify.core.dagger.modules.StoryModule;
import barter.swapify.core.dagger.modules.SwipeModule;
import dagger.BindsInstance;
import dagger.Component;
import dagger.android.AndroidInjector;
import dagger.android.support.AndroidSupportInjectionModule;

@Singleton
@Component(
        modules = {
                AndroidSupportInjectionModule.class,
                AuthModule.class,
                SwipeModule.class,
                PostModule.class,
                ExploreModule.class,
                StoryModule.class,
                MessengerModule.class,
                ChatModule.class
        }
)
public interface AppComponent extends AndroidInjector<BaseApplication> {
    @Component.Builder
    interface Builder {
        @BindsInstance
        Builder application(Application application);

        @BindsInstance
        Builder context(Context context);

        AppComponent build();
    }
}
