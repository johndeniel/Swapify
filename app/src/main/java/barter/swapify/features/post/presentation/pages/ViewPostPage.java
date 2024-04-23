package barter.swapify.features.post.presentation.pages;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import barter.swapify.R;
import barter.swapify.core.credential.domain.entity.CredentialEntity;
import barter.swapify.core.credential.presentation.notifiers.CredentialNotifiers;
import barter.swapify.core.errors.Failure;
import barter.swapify.core.typedef.Either;
import barter.swapify.features.post.domain.entity.PostEntity;
import barter.swapify.features.post.domain.repository.PostRepository;
import barter.swapify.features.post.domain.usecases.FetchUseCases;
import barter.swapify.features.post.presentation.notifiers.ViewPostNotifiers;
import barter.swapify.features.post.presentation.widgets.PostAdapter;
import dagger.android.support.AndroidSupportInjection;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class ViewPostPage extends Fragment {

    private static final String TAG = ViewPostPage.class.getSimpleName();
    @Inject
    PostRepository providePostRepository;
    CompositeDisposable compositeDisposable;
    private PostAdapter postAdapter;
    private List<PostEntity> postList;

    private ProgressBar loadingIndicator;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.post_presentation_view_post_page, container, false);
        AndroidSupportInjection.inject(this);
        initializeViews(view);
        compositeDisposable = new CompositeDisposable();
        fetchCredential();
        return view;
    }

    private void initializeViews(View view) {
        loadingIndicator = view.findViewById(R.id.post_view_loading_indicator);
        RecyclerView recyclerView = view.findViewById(R.id.view_post_recycler_view);
        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        postList = new ArrayList<>();
        postAdapter = new PostAdapter(postList);
        recyclerView.setAdapter(postAdapter);
    }

    private void fetchCredential() {
        CredentialNotifiers credentialNotifiers = new CredentialNotifiers(requireContext());
        compositeDisposable.add(credentialNotifiers.getCredential()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleCredentialResult));
    }

    private void handleCredentialResult(Either<Failure, CredentialEntity> result) {
        if (result.isRight()) {
            CredentialEntity credentialEntity = result.getRight();
            Log.d(TAG, "Uid: " + credentialEntity.getUid());
            Log.d(TAG, "Email: " + credentialEntity.getEmail());
            Log.d(TAG, "Display Name: " + credentialEntity.getDisplayName());
            Log.d(TAG, "Photo Url: " + credentialEntity.getPhotoUrl());
            fetchItems(credentialEntity.getUid());
        }
    }

    private void fetchItems(String uid) {
        showLoading();
        ViewPostNotifiers postNotifiers = new ViewPostNotifiers(uid, new FetchUseCases(providePostRepository));
        compositeDisposable.add(postNotifiers.fetch()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleSwipeFetchResult));
    }

    @SuppressLint("NotifyDataSetChanged")
    private void handleSwipeFetchResult(Either<Failure, List<PostEntity>> result) {
        if (result.isRight()) {
            List<PostEntity> fetchedItems = result.getRight();
            if (fetchedItems != null) {
                postList.clear();
                postList.addAll(fetchedItems);
                postAdapter.notifyDataSetChanged();
                hideLoading();
            }
        } else if (result.isLeft()) {
            Failure failure = result.getLeft();
           // showSnackBar(failure.getErrorMessage());
        }
    }

    private void hideLoading() {
        loadingIndicator.setVisibility(View.GONE);
    }

    private void showLoading() {
        loadingIndicator.setVisibility(View.VISIBLE);
    }
}