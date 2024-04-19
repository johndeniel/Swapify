package barter.swapify.features.explore.presentation.pages;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.facebook.shimmer.ShimmerFrameLayout;

import java.util.List;

import javax.inject.Inject;

import barter.swapify.R;
import barter.swapify.core.errors.Failure;
import barter.swapify.core.typedef.Either;
import barter.swapify.core.widgets.shimmer.GlideShimmerHelper;
import barter.swapify.core.widgets.snackbar.SnackBarHelper;
import barter.swapify.features.explore.domain.repository.ExploreRepository;
import barter.swapify.features.explore.domain.usecases.BannerUseCases;
import barter.swapify.features.explore.presentation.notifiers.ExploreNotifiers;
import dagger.android.support.AndroidSupportInjection;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class ExplorePage extends Fragment {
    private static final String TAG = ExplorePage.class.getSimpleName();

    @Inject ExploreRepository exploreRepository;
    private CompositeDisposable compositeDisposable;
    private ImageView topBanner;
    private ImageView bottomBanner;
    private ImageView bannerSection1;
    private ImageView bannerSection2;
    private ImageView bannerSection3;
    private ImageView bannerSection4;

    private ShimmerFrameLayout topBannerShimmer;
    private ShimmerFrameLayout bottomBannerShimmer;
    private ShimmerFrameLayout bannerSection1Shimmer;
    private ShimmerFrameLayout bannerSection2Shimmer;
    private ShimmerFrameLayout bannerSection3Shimmer;
    private ShimmerFrameLayout bannerSection4Shimmer;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.explore_presentation_explore_page, container, false);
        AndroidSupportInjection.inject(this);
        compositeDisposable = new CompositeDisposable();

        initViews(view);
        fetchBanner();
        return view;
    }

    private void fetchBanner() {
        ExploreNotifiers exploreNotifiers = new ExploreNotifiers(new BannerUseCases(exploreRepository));
        compositeDisposable.add(exploreNotifiers.banner()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleBannerResult, this::handleError));
    }

    private void initViews(View view) {
        topBannerShimmer = view.findViewById(R.id.top_banner_shimmer);
        bottomBannerShimmer = view.findViewById(R.id.bottom_banner_shimmer);
        bannerSection1Shimmer = view.findViewById(R.id.banner_section_1_shimmer);
        bannerSection2Shimmer = view.findViewById(R.id.banner_section_2_shimmer);
        bannerSection3Shimmer = view.findViewById(R.id.banner_section_3_shimmer);
        bannerSection4Shimmer = view.findViewById(R.id.banner_section_4_shimmer);

        topBanner = view.findViewById(R.id.top_banner);
        bottomBanner = view.findViewById(R.id.bottom_banner);
        bannerSection1 = view.findViewById(R.id.banner_section_1);
        bannerSection2 = view.findViewById(R.id.banner_section_2);
        bannerSection3 = view.findViewById(R.id.banner_section_3);
        bannerSection4 = view.findViewById(R.id.banner_section_4);
    }

    private void handleBannerResult(Either<Failure, List<String>> result) {
        if (result.isRight()) {
            List<String> itemList = result.getRight();
            for (int i = 0; i < itemList.size() && i < 6; i++) {
                String imageUrl = itemList.get(i);
                loadBannerImage(imageUrl, getBannerLayout(i));
            }
        } else {
            showSnackBar(result.getLeft().getErrorMessage());
        }
    }

    private ImageView getBannerLayout(int index) {
        switch (index) {
            case 0: return topBanner;
            case 1: return bottomBanner;
            case 2: return bannerSection1;
            case 3: return bannerSection2;
            case 4: return bannerSection3;
            case 5: return bannerSection4;
            default: return null;
        }
    }

    private void loadBannerImage(String imageUrl, ImageView imageView) {
        ShimmerFrameLayout shimmerLayout = getShimmerLayout(imageView);
        if (shimmerLayout != null) {
            Glide.with(this)
                    .load(imageUrl)
                    .centerCrop()
                    .listener(new GlideShimmerHelper(shimmerLayout))
                    .placeholder(R.drawable.rectangle)
                    .into(imageView);
        }
    }

    private ShimmerFrameLayout getShimmerLayout(ImageView imageView) {
        if (imageView == topBanner) return topBannerShimmer;
        else if (imageView == bottomBanner) return bottomBannerShimmer;
        else if (imageView == bannerSection1) return bannerSection1Shimmer;
        else if (imageView == bannerSection2) return bannerSection2Shimmer;
        else if (imageView == bannerSection3) return bannerSection3Shimmer;
        else if (imageView == bannerSection4) return bannerSection4Shimmer;
        else return null;
    }

    private void showSnackBar(String message) {
        SnackBarHelper.invoke(message, requireView());
    }

    private void handleError(Throwable throwable) {
        Log.e(TAG, "Error loading banners: " + throwable.getMessage());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        compositeDisposable.dispose();
    }
}
