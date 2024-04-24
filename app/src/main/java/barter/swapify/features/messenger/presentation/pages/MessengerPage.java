package barter.swapify.features.messenger.presentation.pages;

import static barter.swapify.core.constants.Constants.CHATROOM_REFERENCE;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import javax.inject.Inject;

import barter.swapify.R;
import barter.swapify.core.credential.domain.entity.CredentialEntity;
import barter.swapify.core.credential.presentation.notifiers.CredentialNotifiers;
import barter.swapify.core.errors.Failure;
import barter.swapify.core.typedef.Either;
import barter.swapify.features.messenger.domain.entity.Chatroom;
import barter.swapify.features.messenger.domain.repository.MessengerRepository;
import barter.swapify.features.messenger.presentation.widgets.RecentChatRecyclerAdapter;
import barter.swapify.features.story.presentation.pages.StoryPage;
import dagger.android.support.AndroidSupportInjection;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MessengerPage extends Fragment {

    private static final String TAG = MessengerPage.class.getSimpleName();
    @Inject
    MessengerRepository provideMessengerRepository;

    private CompositeDisposable compositeDisposable;
    private RecyclerView recentChatRecyclerView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.messenger_presentation_messenger_page, container, false);
        AndroidSupportInjection.inject(this);

        initViews(view);
        fetchCredential();
        return view;
    }

    private void initViews(View view) {
        replaceFragment(new StoryPage());
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
            fetchItems(credentialEntity.getUid());
        } else {
            Log.e(TAG, "Failed to fetch credential: " + result.getLeft().getErrorMessage());
        }
    }

    private void fetchItems(String uid) {
        Query query = FirebaseFirestore.getInstance().collection(CHATROOM_REFERENCE)
                .whereArrayContains("userIds", uid)
                .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING);

        FirestoreRecyclerOptions<Chatroom> options = new FirestoreRecyclerOptions.Builder<Chatroom>()
                .setQuery(query, Chatroom.class)
                .build();

        RecentChatRecyclerAdapter recentChatRecyclerAdapter = new RecentChatRecyclerAdapter(options, requireContext(), uid);
        recentChatRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recentChatRecyclerView.setAdapter(recentChatRecyclerAdapter);
        recentChatRecyclerAdapter.startListening();
    }



    private void replaceFragment(Fragment fragment) {
        if (fragment != null) {
            FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
            transaction.replace(R.id.story_container, fragment);
            transaction.commit();
        }
    }

    private void handleError(Throwable throwable) {
        Log.e(TAG, "Error: " + throwable.getMessage());
        // Handle error as needed, e.g., show error message to user
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        compositeDisposable.clear(); // Dispose of disposables to avoid memory leaks
    }
}
