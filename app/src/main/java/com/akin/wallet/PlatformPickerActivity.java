package com.akin.wallet;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.akin.wallet.adapter.PlatformSelectionAdapter;
import com.akin.wallet.model.PlatformOption;

import java.util.ArrayList;
import java.util.List;

/**
 * Full-screen platform picker (replaces the old bottom sheet). Returns the
 * chosen platform icon and name; the caller keeps its own search/filter.
 */
public class PlatformPickerActivity extends AppCompatActivity {

    public static final String EXTRA_ICON_RES = "extra_icon_res";
    public static final String EXTRA_NAME = "extra_name";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_social_platform);

        RecyclerView recyclerPlatforms = findViewById(R.id.recycler_platforms);
        recyclerPlatforms.setLayoutManager(new LinearLayoutManager(this));
        PlatformSelectionAdapter platformAdapter = new PlatformSelectionAdapter(getPlatforms(),
                (iconRes, name, url) -> {
                    Intent data = new Intent();
                    data.putExtra(EXTRA_ICON_RES, iconRes);
                    data.putExtra(EXTRA_NAME, name);
                    setResult(RESULT_OK, data);
                    finish();
                });
        recyclerPlatforms.setAdapter(platformAdapter);

        EditText searchPlatform = findViewById(R.id.search_platform);
        searchPlatform.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                platformAdapter.getFilter().filter(s);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private static List<PlatformOption> getPlatforms() {
        List<PlatformOption> platforms = new ArrayList<>();
        platforms.add(new PlatformOption(R.drawable.binance, "Binance", "binance.com"));
        platforms.add(new PlatformOption(R.drawable.bdo, "BDO", "bdo.com.ph"));
        platforms.add(new PlatformOption(R.drawable.bpi, "BPI", "bpi.com.ph"));
        platforms.add(new PlatformOption(R.drawable.discord, "Discord", "discord.com"));
        platforms.add(new PlatformOption(R.drawable.dropbox, "Dropbox", "dropbox.com"));
        platforms.add(new PlatformOption(R.drawable.facebook, "Facebook", "facebook.com"));
        platforms.add(new PlatformOption(R.drawable.gcash, "GCash", "gcash.com"));
        platforms.add(new PlatformOption(R.drawable.gitlab, "GitLab", "gitlab.com"));
        platforms.add(new PlatformOption(R.drawable.github, "GitHub", "github.com"));
        platforms.add(new PlatformOption(R.drawable.gmail, "Gmail", "gmail.com"));
        platforms.add(new PlatformOption(R.drawable.gotyme, "GoTyme", "gotyme.com"));
        platforms.add(new PlatformOption(R.drawable.google, "Google", "google.com"));
        platforms.add(new PlatformOption(R.drawable.instagram, "Instagram", "instagram.com"));
        platforms.add(new PlatformOption(R.drawable.itunes, "iTunes", "itunes.com"));
        platforms.add(new PlatformOption(R.drawable.lazada, "Lazada", "lazada.com"));
        platforms.add(new PlatformOption(R.drawable.line, "LINE", "line.me"));
        platforms.add(new PlatformOption(R.drawable.linkedin, "LinkedIn", "linkedin.com"));
        platforms.add(new PlatformOption(R.drawable.maribank, "MariBank", "maribank.com.ph"));
        platforms.add(new PlatformOption(R.drawable.maya, "Maya", "maya.ph"));
        platforms.add(new PlatformOption(R.drawable.messenger, "Messenger", "messenger.com"));
        platforms.add(new PlatformOption(R.drawable.microsoft, "Microsoft", "microsoft.com"));
        platforms.add(new PlatformOption(R.drawable.netflix, "Netflix", "netflix.com"));
        platforms.add(new PlatformOption(R.drawable.paypal, "PayPal", "paypal.com"));
        platforms.add(new PlatformOption(R.drawable.pinterest, "Pinterest", "pinterest.com"));
        platforms.add(new PlatformOption(R.drawable.rcbc, "RCBC", "rcbc.com.ph"));
        platforms.add(new PlatformOption(R.drawable.reddit, "Reddit", "reddit.com"));
        platforms.add(new PlatformOption(R.drawable.shoopee, "Shopee", "shopee.com"));
        platforms.add(new PlatformOption(R.drawable.slack, "Slack", "slack.com"));
        platforms.add(new PlatformOption(R.drawable.snapchat, "Snapchat", "snapchat.com"));
        platforms.add(new PlatformOption(R.drawable.soundcloud, "SoundCloud", "soundcloud.com"));
        platforms.add(new PlatformOption(R.drawable.spotify, "Spotify", "spotify.com"));
        platforms.add(new PlatformOption(R.drawable.steam, "Steam", "store.steampowered.com"));
        platforms.add(new PlatformOption(R.drawable.telegram, "Telegram", "telegram.org"));
        platforms.add(new PlatformOption(R.drawable.tiktok, "TikTok", "tiktok.com"));
        platforms.add(new PlatformOption(R.drawable.tinder, "Tinder", "tinder.com"));
        platforms.add(new PlatformOption(R.drawable.unionbank, "UnionBank", "unionbank.com.ph"));
        platforms.add(new PlatformOption(R.drawable.viber, "Viber", "viber.com"));
        platforms.add(new PlatformOption(R.drawable.wattpad, "Wattpad", "wattpad.com"));
        platforms.add(new PlatformOption(R.drawable.whatsapp, "WhatsApp", "whatsapp.com"));
        platforms.add(new PlatformOption(R.drawable.wise, "Wise", "wise.com"));
        platforms.add(new PlatformOption(R.drawable.x, "X", "x.com"));
        platforms.add(new PlatformOption(R.drawable.youtube, "YouTube", "youtube.com"));
        platforms.add(new PlatformOption(R.drawable.zoom, "Zoom", "zoom.us"));
        return platforms;
    }
}
