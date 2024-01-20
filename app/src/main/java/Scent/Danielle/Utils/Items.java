package Scent.Danielle.Utils;

public class Items {
    private String key;
    private String fullName;
    private String title;
    private String description;
    private String fileName;
    private String imageUrl;

    public Items() {
        // Empty constructor required for Firebase
    }

    public Items(String key, String fullName, String title, String description, String fileName, String imageUrl) {
        this.key = key;
        this.fullName = fullName;
        this.title = title;
        this.description = description;
        this.fileName = fileName;
        this.imageUrl = imageUrl;
    }

    public String getKey() { return key; }

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