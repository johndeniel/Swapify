package Scent.Danielle.Utils;

import android.media.Image;
import android.net.Uri;

public class User {

    private String id;
    private String fullName;
    private Uri avatar;
    public User() {
    }

    public User(String id, String fullName, Uri avatar) {
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
    public Uri getAvatar() {
        return avatar;
    }
}