package barter.swapify.features.swipe.data.datasource;

import androidx.annotation.NonNull;

import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;

import barter.swapify.core.errors.Failure;
import barter.swapify.core.typedef.Either;
import barter.swapify.features.swipe.data.model.SwipeModel;

public class SwipeRemoteDataSourceImpl implements SwipeRemoteDataSource {

    private static final String TAG = SwipeRemoteDataSourceImpl.class.getSimpleName();

    private final DatabaseReference databaseReference;

    @Inject
    public SwipeRemoteDataSourceImpl(DatabaseReference databaseReference) {
        this.databaseReference = databaseReference;
    }

    @Override
    public CompletableFuture<Either<Failure, List<SwipeModel>>> fetch(String uid) {
        CompletableFuture<Either<Failure, List<SwipeModel>>> future = new CompletableFuture<>();

        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<SwipeModel> fetchedItems = new ArrayList<>();

                for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                    SwipeModel item = childSnapshot.getValue(SwipeModel.class);

                    if (item != null && !item.getUserId().equals(uid)) {
                        fetchedItems.add(item);
                        Log.d(TAG, "Fetched SwipeModel: " + item.toString());
                    }
                }

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
}
