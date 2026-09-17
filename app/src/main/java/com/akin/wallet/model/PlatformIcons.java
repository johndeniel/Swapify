package com.akin.wallet.model;

import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.akin.wallet.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Single source of truth for platform branding.
 *
 * <p>Why this exists: {@code R.drawable} ints are reassigned on every build,
 * so the raw ints persisted in {@code logins.icon_res} rot after a reinstall
 * (title says Google, icon shows Facebook). Platform <b>names</b> are stable,
 * so every display site resolves the icon from the name here at bind time and
 * only trusts the stored int for unknown/custom platforms. Old rows heal
 * automatically with no DB migration.
 *
 * <p>Each platform is registered exactly once in the static block below; the
 * icon map and the picker catalog are both derived from it.
 */
public final class PlatformIcons {

    private PlatformIcons() {
    }

    /** Picker row: branding + display url. */
    public static final class Option {
        private final int iconRes;
        private final String name;
        private final String url;

        public Option(int iconRes, String name, String url) {
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

    private static final Map<String, Integer> ICONS = new HashMap<>();
    /** {name, url} pairs in picker order; icons come from {@link #ICONS}. */
    private static final List<String[]> ENTRIES = new ArrayList<>();

    static {
        register(R.drawable.binance, "Binance", "binance.com");
        register(R.drawable.bdo, "BDO", "bdo.com.ph");
        register(R.drawable.bpi, "BPI", "bpi.com.ph");
        register(R.drawable.discord, "Discord", "discord.com");
        register(R.drawable.dropbox, "Dropbox", "dropbox.com");
        register(R.drawable.facebook, "Facebook", "facebook.com");
        register(R.drawable.gcash, "GCash", "gcash.com");
        register(R.drawable.gitlab, "GitLab", "gitlab.com");
        register(R.drawable.github, "GitHub", "github.com");
        register(R.drawable.gmail, "Gmail", "gmail.com");
        register(R.drawable.gotyme, "GoTyme", "gotyme.com");
        register(R.drawable.google, "Google", "google.com");
        register(R.drawable.instagram, "Instagram", "instagram.com");
        register(R.drawable.itunes, "iTunes", "itunes.com");
        register(R.drawable.lazada, "Lazada", "lazada.com");
        register(R.drawable.line, "LINE", "line.me");
        register(R.drawable.linkedin, "LinkedIn", "linkedin.com");
        register(R.drawable.maribank, "MariBank", "maribank.com.ph");
        register(R.drawable.maya, "Maya", "maya.ph");
        register(R.drawable.messenger, "Messenger", "messenger.com");
        register(R.drawable.microsoft, "Microsoft", "microsoft.com");
        register(R.drawable.netflix, "Netflix", "netflix.com");
        register(R.drawable.paypal, "PayPal", "paypal.com");
        register(R.drawable.pinterest, "Pinterest", "pinterest.com");
        register(R.drawable.rcbc, "RCBC", "rcbc.com.ph");
        register(R.drawable.reddit, "Reddit", "reddit.com");
        register(R.drawable.shoopee, "Shopee", "shopee.com");
        register(R.drawable.slack, "Slack", "slack.com");
        register(R.drawable.snapchat, "Snapchat", "snapchat.com");
        register(R.drawable.soundcloud, "SoundCloud", "soundcloud.com");
        register(R.drawable.spotify, "Spotify", "spotify.com");
        register(R.drawable.steam, "Steam", "store.steampowered.com");
        register(R.drawable.telegram, "Telegram", "telegram.org");
        register(R.drawable.tiktok, "TikTok", "tiktok.com");
        register(R.drawable.tinder, "Tinder", "tinder.com");
        register(R.drawable.unionbank, "UnionBank", "unionbank.com.ph");
        register(R.drawable.viber, "Viber", "viber.com");
        register(R.drawable.wattpad, "Wattpad", "wattpad.com");
        register(R.drawable.whatsapp, "WhatsApp", "whatsapp.com");
        register(R.drawable.wise, "Wise", "wise.com");
        register(R.drawable.x, "X", "x.com");
        register(R.drawable.youtube, "YouTube", "youtube.com");
        register(R.drawable.zoom, "Zoom", "zoom.us");
    }

    private static void register(int iconRes, String name, String url) {
        ICONS.put(key(name), iconRes);
        ENTRIES.add(new String[]{name, url});
    }

    private static String key(@Nullable String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.US);
    }

    /**
     * Drawable for a platform name from this build's resources. Never 0:
     * unknown or blank names fall back to the generic social icon.
     */
    public static int iconFor(@Nullable String platformName) {
        return iconFor(platformName, R.drawable.ic_social);
    }

    /**
     * Drawable for a platform name, falling back to a previously stored res id
     * (valid when saved by this same install) and finally the generic icon.
     * Use this when binding rows read from the DB.
     */
    public static int iconFor(@Nullable String platformName, int fallbackRes) {
        Integer mapped = ICONS.get(key(platformName));
        if (mapped != null) {
            return mapped;
        }
        return fallbackRes != 0 ? fallbackRes : R.drawable.ic_social;
    }

    /**
     * Binds a resolved icon into a row, degrading to the generic icon if even
     * the fallback res is stale. One line per adapter instead of a repeated
     * try/catch at every bind site.
     */
    public static void bindIcon(ImageView iconView, @Nullable String platformName, int storedRes) {
        try {
            iconView.setImageResource(iconFor(platformName, storedRes));
        } catch (Exception e) {
            iconView.setImageResource(R.drawable.ic_social);
        }
    }

    /**
     * Full picker catalog (icon + name + url). Fresh list per call because the
     * selection adapter filters it in place.
     */
    public static List<Option> catalog() {
        List<Option> platforms = new ArrayList<>(ENTRIES.size());
        for (String[] entry : ENTRIES) {
            platforms.add(new Option(iconFor(entry[0]), entry[0], entry[1]));
        }
        return platforms;
    }
}
