package barter.swapify.core.credential.data.model;

import barter.swapify.core.credential.domain.entity.CredentialEntity;

public class CredentialModel extends CredentialEntity {
    private final String email;
    private final String displayName;

    public CredentialModel(String email, String displayName) {
        super(email, displayName);
        this.email = email;
        this.displayName = displayName;
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    public static CredentialEntity toCredentialEntity(CredentialModel credentialModel) {
        return new CredentialEntity(credentialModel.getEmail(), credentialModel.getDisplayName());
    }

    public static CredentialModel mapToModel(CredentialEntity credentialEntity) {
        return new CredentialModel(
                credentialEntity.getEmail(),
                credentialEntity.getDisplayName()
        );
    }
}
