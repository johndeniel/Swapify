package com.akin.wallet.model;

public class CredentialItem {
    private final int id;
    private final String platform;
    private final String username;
    private final String password;
    private final String pin;
    private final int iconRes;

    public CredentialItem(String platform, String username, int iconRes) {
        this.id = -1;
        this.platform = platform;
        this.username = username;
        this.password = "";
        this.pin = "";
        this.iconRes = iconRes;
    }

    public CredentialItem(String platform, String username, String password, String pin, int iconRes) {
        this.id = -1;
        this.platform = platform;
        this.username = username;
        this.password = password;
        this.pin = pin;
        this.iconRes = iconRes;
    }

    public CredentialItem(int id, String platform, String username, String password, String pin, int iconRes) {
        this.id = id;
        this.platform = platform;
        this.username = username;
        this.password = password;
        this.pin = pin;
        this.iconRes = iconRes;
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
}
