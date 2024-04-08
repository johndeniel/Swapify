package barter.swapify.core.credential.domain.entity;

public class CredentialEntity {
    private final String email;
    private final String displayName;

    public CredentialEntity(String email, String displayName) {
        this.email = email;
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }
    public String getDisplayName() {
        return displayName;
    }
}
