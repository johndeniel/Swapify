package barter.swapify.features.auth.data.model;

import barter.swapify.features.auth.domain.entity.UserEntity;

public class UserModel extends UserEntity {

    private final String email;
    private final String displayName;

    public UserModel(String email, String displayName) {
        super(email, displayName);
        this.email = email;
        this.displayName = displayName;
    }

    public String getEmail() { return email; }
    public String getDisplayName() {
        return displayName;
    }

    public static UserEntity toUserEntity(UserModel userModel) {
        return new UserEntity( userModel.getEmail(), userModel.getDisplayName());
    }
}
