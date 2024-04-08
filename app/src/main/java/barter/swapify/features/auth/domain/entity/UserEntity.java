package barter.swapify.features.auth.domain.entity;

public class UserEntity {
    private final String displayName;
    private final String email;

    public UserEntity(String email, String displayName) {
        this.email = email;
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
    public String getEmail() {
        return email;
    }
}
