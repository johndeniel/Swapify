package barter.swapify.core.dagger.modules;

import android.content.Context;

import com.google.firebase.auth.FirebaseAuth;

import javax.inject.Singleton;

import barter.swapify.core.network.ConnectionCheckerImpl;
import barter.swapify.features.auth.data.datasource.AuthRemoteDataSource;
import barter.swapify.features.auth.data.datasource.AuthRemoteDataSourceImpl;
import barter.swapify.features.auth.data.repositories.AuthRepositoryImpl;
import barter.swapify.features.auth.domain.repository.AuthRepository;
import barter.swapify.features.auth.presentation.pages.LoginPage;
import dagger.Module;
import dagger.Provides;
import dagger.android.ContributesAndroidInjector;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

@Module
public abstract class AuthModule {

    @ContributesAndroidInjector
    abstract LoginPage contributeLoginPage();

    @Provides
    @Singleton
    static AuthRepository provideAuthRepository(Context context) {
        return new AuthRepositoryImpl(new ConnectionCheckerImpl(context), provideAuthRemoteDataSource());
    }

    @Provides
    @Singleton
    static AuthRemoteDataSource provideAuthRemoteDataSource() {
        return new AuthRemoteDataSourceImpl(FirebaseAuth.getInstance());
    }

    @Provides
    @Singleton
    static CompositeDisposable provideCompositeDisposable() {
        return new CompositeDisposable();
    }
}
