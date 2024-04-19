package barter.swapify.features.swipe.presentation.pages;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import barter.swapify.R;
import barter.swapify.core.credential.domain.entity.CredentialEntity;
import barter.swapify.core.credential.presentation.notifiers.CredentialNotifiers;
import barter.swapify.core.errors.Failure;
import barter.swapify.core.typedef.Either;
import barter.swapify.core.widgets.snackbar.SnackBarHelper;
import barter.swapify.features.swipe.domain.entity.SwipeEntity;
import barter.swapify.features.swipe.domain.repository.SwipeRepository;
import barter.swapify.features.swipe.domain.usecases.FetchUseCases;
import barter.swapify.features.swipe.presentation.notifiers.SwipeNotifiers;
import barter.swapify.features.swipe.presentation.widgets.NonScrollableLayoutManager;
import barter.swapify.features.swipe.presentation.widgets.SwipeAdapter;
import dagger.android.support.AndroidSupportInjection;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class SwipePage extends Fragment {
    private static final String TAG = SwipePage.class.getSimpleName();
    @Inject SwipeRepository swipeRepository;
    @Inject CompositeDisposable compositeDisposable;
    private ProgressBar loadingIndicator;
    private SwipeAdapter swipeAdapter;
    private List<SwipeEntity> swipeList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.swipe_presentation_swipe_page, container, false);
        AndroidSupportInjection.inject(this);
        initializeViews(view);
        fetchCredential();
        return view;
    }

    private void initializeViews(View view) {
        loadingIndicator = view.findViewById(R.id.swipe_loading_indicator);
        RecyclerView recyclerView = view.findViewById(R.id.swipe_recycler_view);
        recyclerView.setLayoutManager(new NonScrollableLayoutManager(requireContext()));
        swipeList = new ArrayList<>();
        swipeAdapter = new SwipeAdapter(swipeList);
        recyclerView.setAdapter(swipeAdapter);
        swipeAdapter.attachSwipeHelperToRecyclerView(recyclerView);
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
        SwipeNotifiers swipeNotifiers = new SwipeNotifiers(uid, new FetchUseCases(swipeRepository));
        compositeDisposable.add(swipeNotifiers.fetch()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleSwipeFetchResult));
    }

    @SuppressLint("NotifyDataSetChanged")
    private void handleSwipeFetchResult(Either<Failure, List<SwipeEntity>> result) {
        if (result.isRight()) {
            List<SwipeEntity> fetchedItems = result.getRight();
            if (fetchedItems != null) {
                swipeList.clear();
                swipeList.addAll(fetchedItems);
                swipeAdapter.notifyDataSetChanged();
                hideLoading();
            }
        } else if (result.isLeft()) {
            Failure failure = result.getLeft();
            showSnackBar(failure.getErrorMessage());
        }
    }

    private void hideLoading() {
        loadingIndicator.setVisibility(View.GONE);
    }

    private void showLoading() {
        loadingIndicator.setVisibility(View.VISIBLE);
    }

    private void showSnackBar(String message) {
        SnackBarHelper.invoke(message, requireView());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        compositeDisposable.clear();
    }
}
