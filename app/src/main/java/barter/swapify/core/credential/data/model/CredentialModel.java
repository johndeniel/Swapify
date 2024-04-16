package barter.swapify.core.credential.data.model;

import barter.swapify.core.credential.domain.entity.CredentialEntity;

public class CredentialModel extends CredentialEntity {
    private final String uid;
    private final String email;
    private final String displayName;
    private final String photoUrl;

    public CredentialModel(String uid, String email, String displayName, String photoUrl) {
        super(uid, email, displayName, photoUrl);
        this.uid = uid;
        this.email = email;
        this.displayName = displayName;
        this.photoUrl = photoUrl;
    }

    @Override
    public String getUid() {
        return uid;
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getPhotoUrl() {
        return photoUrl;
    }

    public static CredentialEntity toCredentialEntity(CredentialModel credentialModel) {
        return new CredentialEntity(
                credentialModel.getUid(),
                credentialModel.getEmail(),
                credentialModel.getDisplayName(),
                credentialModel.getPhotoUrl()
        );
    }

    public static CredentialModel mapToModel(CredentialEntity credentialEntity) {
        return new CredentialModel(
                credentialEntity.getUid(),
                credentialEntity.getEmail(),
                credentialEntity.getDisplayName(),
                credentialEntity.getPhotoUrl()
        );
    }
}
