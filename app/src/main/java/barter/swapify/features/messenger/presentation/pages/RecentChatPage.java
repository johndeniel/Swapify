package barter.swapify.features.messenger.presentation.pages;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;

import javax.inject.Inject;

import barter.swapify.R;
import barter.swapify.core.credential.domain.entity.CredentialEntity;
import barter.swapify.core.credential.presentation.notifiers.CredentialNotifiers;
import barter.swapify.core.errors.Failure;
import barter.swapify.core.typedef.Either;
import barter.swapify.features.messenger.domain.entity.Chatroom;
import barter.swapify.features.messenger.domain.repository.MessengerRepository;
import barter.swapify.features.messenger.domain.usecases.RecentUseCases;
import barter.swapify.features.messenger.presentation.notifiers.MessengerNotifiers;
import barter.swapify.features.messenger.presentation.widgets.ChatHeadAdapter;
import dagger.android.support.AndroidSupportInjection;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class RecentChatPage extends Fragment {

    private static final String TAG = RecentChatPage.class.getSimpleName();
    @Inject
    MessengerRepository provideMessengerRepository;
    private CompositeDisposable compositeDisposable;
    private RecyclerView recentChatRecyclerView;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.messenger_presentation_recent_chat_page, container, false);
        AndroidSupportInjection.inject(this);

        initViews(view);
        fetchCredential();
        return view;
    }

    private void initViews(View view) {
        compositeDisposable = new CompositeDisposable();
        recentChatRecyclerView = view.findViewById(R.id.recentChats);
    }

    private void fetchCredential() {
        CredentialNotifiers credentialNotifiers = new CredentialNotifiers(requireContext());
        compositeDisposable.add(credentialNotifiers.getCredential()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleCredentialResult, this::handleError));
    }

    private void handleCredentialResult(Either<Failure, CredentialEntity> result) {
        if (result.isRight()) {
            CredentialEntity credentialEntity = result.getRight();
            Log.d(TAG, "Uid: " + credentialEntity.getUid());
            Log.d(TAG, "Email: " + credentialEntity.getEmail());
            Log.d(TAG, "Display Name: " + credentialEntity.getDisplayName());
            Log.d(TAG, "Photo Url: " + credentialEntity.getPhotoUrl());
            handleChatRoom(credentialEntity.getUid());
        } else {
            Log.e(TAG, "Failed to fetch credential: " + result.getLeft().getErrorMessage());
        }
    }


    private void handleChatRoom(String uid) {
        MessengerNotifiers exploreNotifiers = new MessengerNotifiers(uid, new RecentUseCases(provideMessengerRepository));
        compositeDisposable.add(exploreNotifiers.recent()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::displayChatRoom, this::handleError));
    }

    private void displayChatRoom(Either<Failure, FirestoreRecyclerOptions<Chatroom>> result) {
        if (result.isRight()) {
            ChatHeadAdapter recentChatRecyclerAdapter = new ChatHeadAdapter(result.getRight(), "RZCVBq2uI6SErP4BUcC0qS8G4Az2");
            recentChatRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
            recentChatRecyclerView.setAdapter(recentChatRecyclerAdapter);
            recentChatRecyclerAdapter.startListening();
        }
    }

    private void handleError(Throwable throwable) {
        Log.e(TAG, "Error: " + throwable.getMessage());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        compositeDisposable.clear();
    }
}
