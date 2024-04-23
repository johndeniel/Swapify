package barter.swapify.features.post.data.datasource;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;

import barter.swapify.core.errors.Failure;
import barter.swapify.core.typedef.Either;
import barter.swapify.features.post.data.model.PostModel;

public class PostRemoteDataSourceImpl implements PostRemoteDataSource {
    private static final String TAG = PostRemoteDataSourceImpl.class.getSimpleName();

    private final DatabaseReference databaseReference;

    @Inject
    public PostRemoteDataSourceImpl(DatabaseReference databaseReference) {
        this.databaseReference = databaseReference;
    }

    @Override
    public CompletableFuture<Either<Failure, List<PostModel>>> fetch(String uid) {
        CompletableFuture<Either<Failure, List<PostModel>>> future = new CompletableFuture<>();
        Query query = databaseReference.orderByChild("userId").equalTo(uid);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<PostModel> fetchedItems = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    PostModel item = snapshot.getValue(PostModel.class);
                    if (item != null) {
                        fetchedItems.add(item);
                    }
                }

                // Reverse the list
                Collections.reverse(fetchedItems);

                Log.d(TAG, "Fetch operation completed. Fetched items count: " + fetchedItems.size());
                future.complete(Either.right(fetchedItems));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Fetch operation cancelled with error: " + error.getMessage());
                future.complete(Either.left(new Failure(error.getMessage())));
            }
        });
        return future;
    }

    @Override
    public CompletableFuture<Either<Failure, List<PostModel>>> heart() {
        CompletableFuture<Either<Failure, List<PostModel>>> future = new CompletableFuture<>();
        List<PostModel> dummyPosts = new ArrayList<>();
        dummyPosts.add(createPostModel("https://images.unsplash.com/photo-1521499892833-773a6c6fd0b8?q=80&w=2070&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D"));
        dummyPosts.add(createPostModel("https://images.unsplash.com/photo-1621265010303-a793d1017307?q=80&w=2070&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D"));
        dummyPosts.add(createPostModel("https://images.unsplash.com/photo-1591152231320-bc7902cf852e?q=80&w=1776&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D"));
        dummyPosts.add(createPostModel("https://images.unsplash.com/photo-1710880694004-4da9ea2a2c44?q=80&w=2072&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D"));
        dummyPosts.add(createPostModel("https://images.unsplash.com/photo-1612547036242-77002603e5aa?q=80&w=2070&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D"));


        future.complete(Either.right(dummyPosts));
        return future;
    }

    @Override
    public CompletableFuture<Either<Failure, List<PostModel>>> tag() {
        CompletableFuture<Either<Failure, List<PostModel>>> future = new CompletableFuture<>();
        List<PostModel> dummyPosts = new ArrayList<>();
        dummyPosts.add(createPostModel("https://images.unsplash.com/photo-1521058001910-55b77aba2203?q=80&w=2067&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D"));
        dummyPosts.add(createPostModel("https://images.unsplash.com/photo-1508599589920-14cfa1c1fe4d?q=80&w=2003&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D"));
        dummyPosts.add(createPostModel("https://images.unsplash.com/photo-1567848117389-76f5a6d955ae?q=80&w=2072&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D"));
        dummyPosts.add(createPostModel("https://images.unsplash.com/photo-1585184394271-4c0a47dc59c9?q=80&w=2071&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D"));
        dummyPosts.add(createPostModel("https://images.unsplash.com/photo-1554283180-58b016d34e7c?q=80&w=1887&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D"));

        future.complete(Either.right(dummyPosts));
        return future;
    }

    private PostModel createPostModel(String imageUrl) {
        return new PostModel(null, null, null, null, null, null, null, imageUrl);
    }
}
