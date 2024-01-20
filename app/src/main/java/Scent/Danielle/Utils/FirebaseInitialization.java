package Scent.Danielle.Utils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class FirebaseInitialization {
    private static final String ITEMS_REFERENCE = "items";
    private static final String GALLERY_REFERENCE = "gallery";

    // Firebase User Information
    private static final FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
    private static final FirebaseUser currentUser = firebaseAuth.getCurrentUser();
    private static final String userId = currentUser.getUid();


    // Firebase Realtime Database
    private static final FirebaseDatabase database = FirebaseDatabase.getInstance();
    private static final DatabaseReference itemsDatabaseReference = database.getReference(ITEMS_REFERENCE);
    private static final DatabaseReference galleryDatabaseReference = database.getReference(GALLERY_REFERENCE).child(userId);


    // Firebase Storage
    private static final FirebaseStorage storage = FirebaseStorage.getInstance();
    private static final StorageReference storageReference = storage.getReference();


    public static String getUserId() {
        return userId;
    }
    public static DatabaseReference getItemsDatabaseReference() {
        return itemsDatabaseReference;
    }
    public static DatabaseReference getGalleryDatabaseReference() {
        return galleryDatabaseReference;
    }
    public static StorageReference getStorageReference() {
        return storageReference;
    }
}