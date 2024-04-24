package barter.swapify.features.story.presentation.pages;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import barter.swapify.R;
import barter.swapify.core.errors.Failure;
import barter.swapify.core.typedef.Either;

import barter.swapify.features.story.domain.entity.StoryEntity;
import barter.swapify.features.story.domain.repository.StoryRepository;
import barter.swapify.features.story.domain.usecases.FetchUseCases;
import barter.swapify.features.story.presentation.notifiers.StoryNotifiers;
import barter.swapify.features.story.presentation.widgets.StoryAdapter;
import dagger.android.support.AndroidSupportInjection;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class StoryPage extends Fragment {

    private static final String TAG = StoryPage.class.getSimpleName();
    @Inject
    StoryRepository provideStoryRepository;
    CompositeDisposable compositeDisposable;
    private StoryAdapter postAdapter;
    private List<StoryEntity> postList;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.story_presentation_story_page, container, false);
        compositeDisposable = new CompositeDisposable();
        AndroidSupportInjection.inject(this);
        initializeViews(view);
        return view;
    }


    private void initializeViews(View view) {
        RecyclerView recyclerView = view.findViewById(R.id.story_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        postList = new ArrayList<>();
        postAdapter = new StoryAdapter(postList);
        recyclerView.setAdapter(postAdapter);
        fetchItems();
    }
    private void fetchItems() {
        StoryNotifiers storyNotifiers = new StoryNotifiers(new FetchUseCases(provideStoryRepository));
        compositeDisposable.add(storyNotifiers.fetch()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleSwipeFetchResult));
    }

    @SuppressLint("NotifyDataSetChanged")
    private void handleSwipeFetchResult(Either<Failure, List<StoryEntity>> result) {
        if (result.isRight()) {
            List<StoryEntity> fetchedItems = result.getRight();
            if (fetchedItems != null) {
                postList.clear();
                postList.addAll(fetchedItems);
                postAdapter.notifyDataSetChanged();
            }
        }
    }
}
