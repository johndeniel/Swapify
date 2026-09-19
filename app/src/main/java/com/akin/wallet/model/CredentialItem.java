package com.akin.wallet.model;

import java.util.Objects;

public class CredentialItem {
    private final int id;
    private final String platform;
    private final String username;
    private final String password;
    private final String pin;
    private final int iconRes;
    // Contact number stored as its own column (never inside another field).
    // Empty string when not provided.
    private final String mobile;
    // Epoch millis (UTC). 0 = unset (unsaved drafts); the DB fills real
    // values on insert.
    private final long createdAt;
    private final long updatedAt;

    public CredentialItem(String platform, String username, String password, String pin, int iconRes,
                          String mobile, long createdAt, long updatedAt) {
        this.id = -1;
        this.platform = platform;
        this.username = username;
        this.password = password;
        this.pin = pin;
        this.iconRes = iconRes;
        this.mobile = mobile != null ? mobile : "";
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public CredentialItem(int id, String platform, String username, String password, String pin, int iconRes,
                          String mobile, long createdAt, long updatedAt) {
        this.id = id;
        this.platform = platform;
        this.username = username;
        this.password = password;
        this.pin = pin;
        this.iconRes = iconRes;
        this.mobile = mobile != null ? mobile : "";
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

    public String getMobile() {
        return mobile;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    /** Value equality across every column (backs DiffUtil content checks). */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CredentialItem)) {
            return false;
        }
        CredentialItem that = (CredentialItem) o;
        return id == that.id
                && iconRes == that.iconRes
                && createdAt == that.createdAt
                && updatedAt == that.updatedAt
                && Objects.equals(platform, that.platform)
                && Objects.equals(username, that.username)
                && Objects.equals(password, that.password)
                && Objects.equals(pin, that.pin)
                && Objects.equals(mobile, that.mobile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, platform, username, password, pin, iconRes,
                mobile, createdAt, updatedAt);
    }
}
