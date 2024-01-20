package Scent.Danielle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import Scent.Danielle.Utils.ItemGalleryAdapter;
import Scent.Danielle.Utils.Items;

public class GalleryActivity extends Fragment {

    private static final String ITEMS_REFERENCE = "items";
    private static final String GALLERY_REFERENCE = "gallery";

    private DatabaseReference databaseReference;
    private FirebaseAuth firebaseAuth;
    private RecyclerView galleryRecyclerView;
    private ItemGalleryAdapter galleryAdapter;
    private List<Items> itemList;
    private ArrayList<String> childDataList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.activity_gallery, container, false);

        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference(ITEMS_REFERENCE);

        galleryRecyclerView = rootView.findViewById(R.id.galleryRecyclerView);
        setupRecyclerView();

        // Use the callback to ensure proper order of execution
        fetchGalleryData(new DataCallback() {
            @Override
            public void onDataLoaded(ArrayList<String> childDataList) {
                loadItemsFromFirebase(childDataList);
            }
        });

        return rootView;
    }

    private void setupRecyclerView() {
        galleryRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        itemList = new ArrayList<>();
        galleryAdapter = new ItemGalleryAdapter(requireContext() ,itemList);
        galleryRecyclerView.setAdapter(galleryAdapter);
    }

    private void fetchGalleryData(final DataCallback callback) {
        ArrayList<String> childDataList = new ArrayList<>();

        DatabaseReference galleryReference = FirebaseDatabase.getInstance()
                .getReference(GALLERY_REFERENCE)
                .child(firebaseAuth.getCurrentUser().getUid());

        galleryReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String childData = snapshot.getValue(String.class);
                    childDataList.add(childData);
                    Log.d("ChildData", "Added: " + childData);
                }
                callback.onDataLoaded(childDataList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("FirebaseError", "Error: " + databaseError.getMessage());
            }
        });
    }

    private void loadItemsFromFirebase(final ArrayList<String> childDataList) {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                itemList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    if (childDataList.contains(snapshot.getKey())) {
                        Items item = snapshot.getValue(Items.class);
                        itemList.add(item);
                    }
                }
                galleryAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(requireActivity(), "Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Define a callback interface
    interface DataCallback {
        void onDataLoaded(ArrayList<String> childDataList);
    }
}
