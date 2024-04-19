package barter.swapify.features.explore.data.datasource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import barter.swapify.core.errors.Failure;
import barter.swapify.core.typedef.Either;
import barter.swapify.features.swipe.data.model.SwipeModel;

public class ExploreRemoteDataSourceImpl implements ExploreRemoteDataSource {

    private static final String TAG = ExploreRemoteDataSourceImpl.class.getSimpleName();
    @Override
    public CompletableFuture<Either<Failure, List<String>>> banner() {
        CompletableFuture<Either<Failure, List<String>>> future = new CompletableFuture<>();

        List<String> fetchedBanner = new ArrayList<>();
        fetchedBanner.add("https://firebasestorage.googleapis.com/v0/b/scent-danielle.appspot.com/o/asset%2Ftop_banner.png?alt=media&token=743f4397-c35b-4344-a3e3-f62aedc35667");
        fetchedBanner.add("https://firebasestorage.googleapis.com/v0/b/scent-danielle.appspot.com/o/asset%2Flower_banner.png?alt=media&token=a92d7287-7fae-4341-9f03-7effe9fc3101");
        fetchedBanner.add("https://firebasestorage.googleapis.com/v0/b/scent-danielle.appspot.com/o/asset%2Fbanner_section_1.png?alt=media&token=a14e6c10-070b-4ef2-9972-e1b9bfd2d07e");
        fetchedBanner.add("https://firebasestorage.googleapis.com/v0/b/scent-danielle.appspot.com/o/asset%2Fbanner_section_2.png?alt=media&token=81b61382-1799-45b1-81bf-2c23db0f1c4f");
        fetchedBanner.add("https://firebasestorage.googleapis.com/v0/b/scent-danielle.appspot.com/o/asset%2Fbanner_section_3.png?alt=media&token=eb030ab9-a36d-4b40-b549-3a391457e548");
        fetchedBanner.add("https://firebasestorage.googleapis.com/v0/b/scent-danielle.appspot.com/o/asset%2Fbanner_section_4.png?alt=media&token=e1651908-6bd9-4f7e-a626-b4f91b7c60f0");

        future.complete(Either.right(fetchedBanner));
        return future;
    }
}
