package barter.swapify.features.profile.presentation.pages;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.appcompat.widget.Toolbar;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.tabs.TabLayout;

import barter.swapify.R;
import barter.swapify.core.credential.domain.entity.CredentialEntity;
import barter.swapify.core.credential.presentation.notifiers.CredentialNotifiers;
import barter.swapify.core.errors.Failure;
import barter.swapify.core.typedef.Either;
import barter.swapify.core.widgets.shimmer.GlideShimmerHelper;
import barter.swapify.features.post.presentation.pages.ViewPostPage;
import barter.swapify.features.post.presentation.pages.HeartPostPage;
import barter.swapify.features.post.presentation.pages.TagPostPage;
import barter.swapify.features.settings.presentation.pages.SettingsPage;
import de.hdodenhof.circleimageview.CircleImageView;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class ProfilePage extends Fragment {
    private static final String TAG = ProfilePage.class.getSimpleName();
    private TextView fullName;
    private CircleImageView avatar;
    private Toolbar topAppBar;
    private ShimmerFrameLayout shimmer;
    private CompositeDisposable disposable;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.profile_presentation_profile_page, container, false);
        topAppBar = rootView.findViewById(R.id.topAppBar);
        topAppBar.setOnMenuItemClickListener(this::handleMenuItemClick);
        // Initialize TabLayout
        TabLayout tabLayout = rootView.findViewById(R.id.tabLayout);
        disposable = new CompositeDisposable();
        fullName = rootView.findViewById(R.id.full_name);
        shimmer = rootView.findViewById(R.id.shimmeravatarImageView);
        avatar = rootView.findViewById(R.id.avatarImageView);

        // Set up TabLayout listener
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                // Load corresponding fragment when a tab is selected
                if (tab.getPosition() == 0) {
                    replaceFragment(new ViewPostPage());
                } else if (tab.getPosition() == 1) {
                    replaceFragment(new HeartPostPage());
                } else if (tab.getPosition() == 2) {
                    replaceFragment(new TagPostPage());
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // Optional: Perform action when a tab is unselected
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Optional: Perform action when a tab is reselected
            }
        });

        // Load the default fragment
        replaceFragment(new ViewPostPage());
        fetchCredential();
        return rootView;
    }

    private boolean handleMenuItemClick(MenuItem menuItem) {
        try {
            int itemId = menuItem.getItemId();
            if (itemId == R.id.setting_app_bar) {
                Intent intent = new Intent(requireContext(), SettingsPage.class);
                startActivity(intent);
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling menu item click: " + e.getMessage());
        }
        return false;
    }

    private void replaceFragment(Fragment fragment) {
        if (fragment != null) {
            FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
            transaction.replace(R.id.container, fragment);
            transaction.commit();
        }
    }

    private void fetchCredential() {
        CredentialNotifiers credentialNotifiers = new CredentialNotifiers(requireContext());
        disposable.add(credentialNotifiers.getCredential()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleCredentialResult));
    }

    private void handleCredentialResult(Either<Failure, CredentialEntity> credentialResult) {
        if (credentialResult.isRight()) {
            CredentialEntity credentialEntity = credentialResult.getRight();
            Log.d(TAG, "Uid: " + credentialEntity.getUid());
            Log.d(TAG, "Email: " + credentialEntity.getEmail());
            Log.d(TAG, "Display Name: " + credentialEntity.getDisplayName());
            Log.d(TAG, "Photo Url: " + credentialEntity.getPhotoUrl());

            setupProfileInfo(credentialEntity);
        }
    }

    private void setupProfileInfo(CredentialEntity credentialEntity) {
        Glide.with(requireContext())
                .load(credentialEntity.getPhotoUrl())
                .placeholder(R.drawable.rectangle)
                .centerCrop()
                .listener(new GlideShimmerHelper(shimmer))
                .into(avatar);
        topAppBar.setTitle(convertToUsername(credentialEntity.getEmail()));
        fullName.setText(credentialEntity.getDisplayName());

    }

    private String convertToUsername(String email) {
        // Extract the username from the email (before the @ symbol)
        int atIndex = email.indexOf('@');
        String username = atIndex != -1 ? email.substring(0, atIndex) : email;

        // Remove special characters from the username
        username = username.replaceAll("[^a-zA-Z0-9]", "");

        // Limit the username to a maximum of 15 characters
        if (username.length() > 15) {
            username = username.substring(0, 15);
        }

        return "@"+username;
    }
}
