package barter.swapify.core.credential.data.datasource.dao;

import barter.swapify.core.credential.data.model.CredentialModel;

public interface CredentialDao {
    CredentialModel getCredential();
    void saveCredential(CredentialModel credentialModel);
    boolean logOut();
}
