package barter.swapify.features.swipe.domain.entity;

public class SwipeEntity {
    private String key; // Key of the item in the firebase
    private String userId; // ID of the user who owns the item
    private String avatar; // Avatar of the user who owns the item
    private String fullName; // Full name of the user who owns the item
    private String title; // Title of the item
    private String description; // Description of the item
    private String fileName; // File name of the item (IMAGE)
    private String imageUrl; // URL of the image associated with the item (IMAGE)

    public SwipeEntity() {}

    public SwipeEntity(String key, String userId, String avatar, String fullName, String title, String description, String fileName, String imageUrl) {
        this.key = key;
        this.userId = userId;
        this.avatar = avatar;
        this.fullName = fullName;
        this.title = title;
        this.description = description;
        this.fileName = fileName;
        this.imageUrl = imageUrl;
    }

    public String getKey() {
        return key;
    }

    public String getUserId() {
        return userId;
    }

    public String getAvatar() {
        return avatar;
    }

    public String getFullName() {
        return fullName;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getFileName() {
        return fileName;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}
