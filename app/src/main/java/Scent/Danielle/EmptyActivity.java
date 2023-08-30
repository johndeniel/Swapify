package Scent.Danielle;

import androidx.fragment.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class EmptyActivity extends Fragment {

    public static EmptyActivity newInstance(String text) {
        EmptyActivity fragment = new EmptyActivity();
        Bundle args = new Bundle();
        args.putString("text", text);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.activity_empty, container, false);
        TextView emptyView = rootView.findViewById(R.id.emptyView);

        Bundle args = getArguments();
        if (args != null) {
            String text = args.getString("text");
            emptyView.setText(text);
        }

        return rootView;
    }
}