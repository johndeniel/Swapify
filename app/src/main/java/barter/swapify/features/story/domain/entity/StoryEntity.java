package barter.swapify.features.story.domain.entity;

public class StoryEntity {

    private final String Username;

    private final String AvatarURL;
    private final String PhotoURL;

    public StoryEntity(String username, String avatarURL, String photoURL) {
        Username = username;
        AvatarURL = avatarURL;
        PhotoURL = photoURL;
    }

    public String getUsername() {
        return Username;
    }

    public String getAvatarURL() {
        return AvatarURL;
    }

    public String getPhotoURL() {
        return PhotoURL;
    }

}
