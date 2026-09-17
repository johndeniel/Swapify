package com.akin.wallet.model;

public class CredentialItem {
    private final int id;
    private final String platform;
    private final String username;
    private final String password;
    private final String pin;
    private final int iconRes;
    // Epoch millis (UTC). 0 = unknown (pre-migration rows or unsaved drafts);
    // the DB fills real values on insert.
    private final long createdAt;
    private final long updatedAt;

    public CredentialItem(String platform, String username, String password, String pin, int iconRes) {
        this(platform, username, password, pin, iconRes, 0, 0);
    }

    public CredentialItem(int id, String platform, String username, String password, String pin, int iconRes) {
        this(id, platform, username, password, pin, iconRes, 0, 0);
    }

    public CredentialItem(String platform, String username, String password, String pin, int iconRes,
                          long createdAt, long updatedAt) {
        this.id = -1;
        this.platform = platform;
        this.username = username;
        this.password = password;
        this.pin = pin;
        this.iconRes = iconRes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public CredentialItem(int id, String platform, String username, String password, String pin, int iconRes,
                          long createdAt, long updatedAt) {
        this.id = id;
        this.platform = platform;
        this.username = username;
        this.password = password;
        this.pin = pin;
        this.iconRes = iconRes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public int getId() {
        return id;
    }

    public String getPlatform() {
        return platform;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getPin() {
        return pin;
    }

    public int getIconRes() {
        return iconRes;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }
}
