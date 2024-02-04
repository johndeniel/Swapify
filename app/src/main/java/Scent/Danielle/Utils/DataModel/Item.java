package Scent.Danielle.Utils.DataModel;

import java.util.List;

public class Item {
    private String key;
    private String userId;
    private String fullName;
    private String title;
    private String description;
    private String fileName;
    private String imageUrl;

    public Item() {
        // Empty constructor required for Firebase
    }

    public Item(String key, String userId, String fullName, String title, String description, String fileName, String imageUrl) {
        this.key = key;
        this.userId = userId;
        this.fullName = fullName;
        this.title = title;
        this.description = description;
        this.fileName = fileName;
        this.imageUrl = imageUrl;
    }

    public String getKey() { return key; }

    public String getUserId() { return userId; }

    public String getFullName() {
        return fullName;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getFileName() { return fileName; }

    public String getImageUrl() { return imageUrl; }
}