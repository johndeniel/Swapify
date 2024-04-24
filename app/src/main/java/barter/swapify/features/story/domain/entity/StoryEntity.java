package barter.swapify.features.story.domain.entity;

public class StoryEntity {

    private final String Username;
    private final String PhotoURL;

    public StoryEntity(String username, String photoURL) {
        Username = username;
        PhotoURL = photoURL;
    }

    public String getUsername() {
        return Username;
    }

    public String getPhotoURL() {
        return PhotoURL;
    }

}
