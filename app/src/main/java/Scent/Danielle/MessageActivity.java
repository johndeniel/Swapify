package Scent.Danielle;

// Android core components
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

// AndroidX components
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

// Java standard imports
import java.util.ArrayList;
import java.util.List;

// Custom class imports
import Scent.Danielle.Utils.ChatAdapter;
import Scent.Danielle.Utils.ChatItem;

public class MessageActivity extends Fragment {

    public MessageActivity() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.activity_message, container, false);

        // Create sample chat items
        List<ChatItem> chatItems = new ArrayList<>();
        chatItems.add(new ChatItem(R.drawable.about, "Alice", "Hi there! How's your day going? Everything alright?"));
        chatItems.add(new ChatItem(R.drawable.about, "Bob", "Hey, how's it going? Long time no see!"));
        chatItems.add(new ChatItem(R.drawable.about, "Charlie", "Hello from the other side. How's your family?"));
        chatItems.add(new ChatItem(R.drawable.about, "David", "Long time no chat! We should catch up sometime."));
        chatItems.add(new ChatItem(R.drawable.about, "Emily", "What's new with you? I heard you got a new job!"));
        chatItems.add(new ChatItem(R.drawable.about, "Frank", "How's your day been? I'm planning a trip next month."));
        chatItems.add(new ChatItem(R.drawable.about, "Grace", "Hey there! Remember that party last weekend? It was amazing!"));
        chatItems.add(new ChatItem(R.drawable.about, "Henry", "Hi, hope you're doing well. I'm excited for the upcoming concert."));
        chatItems.add(new ChatItem(R.drawable.about, "Ivy", "Hey, I found a great recipe. Let's try it when we meet."));
        chatItems.add(new ChatItem(R.drawable.about, "Jack", "Hello! I just finished reading that book you recommended."));
        chatItems.add(new ChatItem(R.drawable.about, "Kate", "Hi, how's your pet? I saw the cute pictures you posted."));
        chatItems.add(new ChatItem(R.drawable.about, "Liam", "Hey, did you hear about the new cafe in town? Let's check it out."));

        // Set up RecyclerView
        RecyclerView recyclerView = rootView.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        ChatAdapter adapter = new ChatAdapter(chatItems);
        recyclerView.setAdapter(adapter);

        // Add divider decoration to RecyclerView
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL);
        recyclerView.addItemDecoration(dividerItemDecoration);

        return rootView;
    }
}