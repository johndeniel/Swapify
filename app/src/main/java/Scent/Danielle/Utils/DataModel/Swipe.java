package Scent.Danielle.Utils.DataModel;

public final class Swipe {
    private String userId;
    private String fullName;
    private String photoUrl;
    private boolean like;

    public Swipe() {}

    public Swipe(String userId, String fullName, String photoUrl, boolean like) {
        this.userId = userId;
        this.fullName = fullName;
        this.photoUrl = photoUrl;
        this.like = like;
    }

    public String getUserId() {
        return userId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }
    public boolean isLike() {
        return like;
    }
}
