package Scent.Danielle.Utils.DataModel;

public class Item {
    private String key; // Key of the item in the firebase
    private String userId; // ID of the user who owns the item
    private String avatar; // Avatar of the user who owns the item
    private String fullName; // Full name of the user who owns the item
    private String title; // Title of the item
    private String description; // Description of the item
    private String fileName; // File name of the item (IMAGE)
    private String imageUrl; // URL of the image associated with the item (IMAGE)

    /**
     * Default constructor required for Firebase.
     */
    public Item() {
        // Empty constructor required for Firebase
    }

    /**
     * Constructor to initialize an Item object.
     * @param key The key of the item.
     * @param userId The ID of the user who owns the item.
     * @param avatar The avatar of the user who owns the item.
     * @param fullName The full name of the user who owns the item.
     * @param title The title of the item.
     * @param description The description of the item.
     * @param fileName The file name of the item (if applicable).
     * @param imageUrl The URL of the image associated with the item (if applicable).
     */
    public Item(String key, String userId, String avatar, String fullName, String title, String description, String fileName, String imageUrl) {
        this.key = key;
        this.userId = userId;
        this.avatar = avatar;
        this.fullName = fullName;
        this.title = title;
        this.description = description;
        this.fileName = fileName;
        this.imageUrl = imageUrl;
    }

    /**
     * Get the key of the item.
     * @return The key of the item.
     */
    public String getKey() {
        return key;
    }

    /**
     * Get the ID of the user who owns the item.
     * @return The ID of the user.
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Get the avatar of the user who owns the item.
     * @return The avatar of the user.
     */
    public String getAvatar() {
        return avatar;
    }

    /**
     * Get the full name of the user who owns the item.
     * @return The full name of the user.
     */
    public String getFullName() {
        return fullName;
    }

    /**
     * Get the title of the item.
     * @return The title of the item.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Get the description of the item.
     * @return The description of the item.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Get the file name of the item.
     * @return The file name of the item.
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Get the URL of the image associated with the item.
     * @return The URL of the image.
     */
    public String getImageUrl() {
        return imageUrl;
    }
}