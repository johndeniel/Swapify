package Scent.Danielle.Utils.DataModel;

public final class Swipe {
    private String userId; // ID of the user who performed the swipe
    private String fullName; // Full name of the user who performed the swipe
    private String photoUrl; // URL of the photo associated with the swipe
    private boolean like; // Indicates whether the swipe action is a "like" or not

    /**
     * Default constructor required for Firebase.
     */
    public Swipe() {
        // Empty constructor required for Firebase
    }

    /**
     * Constructor to initialize a Swipe object.
     * @param userId The ID of the user who performed the swipe.
     * @param fullName The full name of the user who performed the swipe.
     * @param photoUrl The URL of the photo associated with the swipe.
     * @param like Indicates whether the swipe action is a "like" or not.
     */
    public Swipe(String userId, String fullName, String photoUrl, boolean like) {
        this.userId = userId;
        this.fullName = fullName;
        this.photoUrl = photoUrl;
        this.like = like;
    }

    /**
     * Get the ID of the user who performed the swipe.
     * @return The ID of the user.
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Get the full name of the user who performed the swipe.
     * @return The full name of the user.
     */
    public String getFullName() {
        return fullName;
    }

    /**
     * Get the URL of the photo associated with the swipe.
     * @return The URL of the photo.
     */
    public String getPhotoUrl() {
        return photoUrl;
    }

    /**
     * Check if the swipe action is a "like".
     * @return True if the swipe is a "like", false otherwise.
     */
    public boolean isLike() {
        return like;
    }
}