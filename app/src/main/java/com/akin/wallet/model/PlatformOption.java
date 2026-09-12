package com.akin.wallet.model;

public class PlatformOption {
    private final int iconRes;
    private final String name;
    private final String url;

    public PlatformOption(int iconRes, String name, String url) {
        this.iconRes = iconRes;
        this.name = name;
        this.url = url;
    }

    public int getIconRes() {
        return iconRes;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }
}
