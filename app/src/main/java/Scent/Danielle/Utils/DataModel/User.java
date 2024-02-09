package Scent.Danielle.Utils.DataModel;

public class User {
    private String id; // ID of the user
    private String fullName; // Full name of the user
    private String avatar; // Avatar of the user

    /**
     * Default constructor required for Firebase.
     */
    public User() {
        // Empty constructor required for Firebase
    }

    /**
     * Constructor to initialize a User object.
     * @param id The ID of the user.
     * @param fullName The full name of the user.
     * @param avatar The avatar of the user.
     */
    public User(String id, String fullName, String avatar) {
        this.id = id;
        this.fullName = fullName;
        this.avatar = avatar;
    }

    /**
     * Get the ID of the user.
     * @return The ID of the user.
     */
    public String getId() {
        return id;
    }

    /**
     * Get the full name of the user.
     * @return The full name of the user.
     */
    public String getFullName() {
        return fullName;
    }

    /**
     * Get the avatar of the user.
     * @return The avatar of the user.
     */
    public String getAvatar() {
        return avatar;
    }
}