package Scent.Danielle.Utils.DataModel;

public class User {
    private String id;
    private String fullName;
    private String avatar;
    public User() {
    }

    public User(String id, String fullName, String avatar) {
        this.id = id;
        this.fullName = fullName;
        this.avatar = avatar;
    }

    public String getId() {
        return id;
    }
    public String getFullName() {
        return fullName;
    }
    public String getAvatar() {
        return avatar;
    }
}