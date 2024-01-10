package Scent.Danielle.Utils;

public class Items {
    private String userId;
    private String fullName;
    private String title;
    private String description;
    private String imageUrl;

    public Items() {
        // Empty constructor required for Firebase
    }

    public Items(String userId, String fullName, String title, String description, String imageUrl) {
        this.userId = userId;
        this.fullName = fullName;
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
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

    public String getImageUrl() {
        return imageUrl;
    }
}