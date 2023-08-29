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

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}