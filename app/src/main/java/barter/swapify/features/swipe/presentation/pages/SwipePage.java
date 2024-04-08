package barter.swapify.features.swipe.presentation.pages;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.Fragment;

import com.facebook.shimmer.ShimmerFrameLayout;

import javax.inject.Inject;

import barter.swapify.R;
import dagger.android.support.AndroidSupportInjection;

public class SwipePage  extends Fragment {

    private ShimmerFrameLayout shimmerFrameLayout;

    @Inject
    String getStrng;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.swipe_presentation_pages_swipe, container, false);
        AndroidSupportInjection.inject(this);

        shimmerFrameLayout = view.findViewById(R.id.shimmer_view_container);
        shimmerFrameLayout.startShimmer();
        return view;
    }
}