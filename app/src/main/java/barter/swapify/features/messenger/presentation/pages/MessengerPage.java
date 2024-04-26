package barter.swapify.features.messenger.presentation.pages;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.tabs.TabLayout;
import barter.swapify.R;
import barter.swapify.features.post.presentation.pages.HeartPostPage;
import barter.swapify.features.story.presentation.pages.StoryPage;
import dagger.android.support.AndroidSupportInjection;
public class MessengerPage extends Fragment {

    private static final String TAG = MessengerPage.class.getSimpleName();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.messenger_presentation_messenger_page, container, false);
        AndroidSupportInjection.inject(this);

        TabLayout tabLayout = view.findViewById(R.id.messanger_tabLayout);
        replaceFragment2(new RecentChatPage());
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    replaceFragment2(new RecentChatPage());
                } else if (tab.getPosition() == 1) {
                    replaceFragment2(new HeartPostPage());
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

        replaceFragment(new StoryPage());
        return view;
    }


    private void replaceFragment2(Fragment fragment) {
        if (fragment != null) {
            FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
            transaction.replace(R.id.messanger_container, fragment);
            transaction.commit();
        }
    }

    private void replaceFragment(Fragment fragment) {
        if (fragment != null) {
            FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
            transaction.replace(R.id.story_container, fragment);
            transaction.commit();
        }
    }
}
