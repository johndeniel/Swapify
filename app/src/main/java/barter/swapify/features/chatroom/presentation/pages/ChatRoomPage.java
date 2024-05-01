package barter.swapify.features.chatroom.presentation.pages;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import barter.swapify.R;
import barter.swapify.core.credential.presentation.notifiers.CredentialNotifiers;
import barter.swapify.core.widgets.snackbar.SnackBarHelper;
import barter.swapify.features.chatroom.domain.repository.ChatRepository;
import barter.swapify.features.chatroom.domain.usecases.FetchUseCases;
import barter.swapify.features.chatroom.domain.usecases.SendUseCases;
import barter.swapify.features.chatroom.presentation.notifiers.ChatNotifier;
import barter.swapify.features.chatroom.presentation.notifiers.FetchNotifier;
import barter.swapify.features.chatroom.presentation.widgets.ConversationAdapter;
import dagger.android.support.DaggerAppCompatActivity;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;


public class ChatRoomPage extends DaggerAppCompatActivity {
    public static final String TAG = ChatRoomPage.class.getSimpleName();
    @Inject
    ChatRepository provideChatRepository;
    private CompositeDisposable compositeDisposable;
    private RecyclerView conversationRecyclerView;
    private EditText composeMessageField;
    private String currentUserUuid;
    private String recipientUuid;
    private String recipientAvatar;
    private String recipientName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_room_page);
        compositeDisposable = new CompositeDisposable();

        // Initialize views
        initializeViews();

        // Get user and recipient information from intent extras
        handleIntentExtras();

        // Setup top app bar with recipient information
        handleSetupTopAppBar();

        // Setup compose message field
        handleSetupComposeAndSendMessageField();

        fetchAllMessages();
    }


    private void initializeViews() {
        conversationRecyclerView = findViewById(R.id.conversationRecyclerView);
        composeMessageField = findViewById(R.id.chat_message_input);
    }

    private void handleIntentExtras() {
        currentUserUuid = "RZCVBq2uI6SErP4BUcC0qS8G4Az2";
        recipientUuid = getIntent().getStringExtra("id");
        showSnackBar(getIntent().getStringExtra("id"));
        recipientName = getIntent().getStringExtra("name");
        recipientAvatar = getIntent().getStringExtra("avatar");
    }

    private void handleSetupTopAppBar() {
        ImageButton backButton = findViewById(R.id.backButton);
        TextView recipientUsernameTextView = findViewById(R.id.recipientUsernameTextView);
        ImageView recipientProfileImageView = findViewById(R.id.recipientProfileImageView);

        backButton.setOnClickListener(view -> onBackPressed());
        recipientUsernameTextView.setText(recipientName);
        Glide.with(this)
                .load(recipientAvatar)
                .into(recipientProfileImageView);
    }

    private void handleSetupComposeAndSendMessageField() {
        ImageButton sendMessageButton = findViewById(R.id.sendMessageButton);

        sendMessageButton.setOnClickListener(v -> {

            String message = composeMessageField.getText().toString().trim();
            if (TextUtils.isEmpty(message)) {
                Log.d(TAG, "Empty message. No action taken.");
                return;
            }

            try {
                sendMessage(message);
            } catch (Exception e) {
                Log.e(TAG, "Error sending message:", e);
            }
        });
    }

    private void fetchCredential() {
        CredentialNotifiers credentialNotifiers = new CredentialNotifiers(this);
        compositeDisposable.add(credentialNotifiers.getCredential()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> {
                            if (result.isRight()) {

                            }else {
                                showSnackBar(result.getLeft().getErrorMessage());
                            }
                        },
                        throwable -> Log.e(TAG, "Error sending messages: " + throwable.getMessage())

                        ));
    }


    private void fetchAllMessages() {
        List<Object> obj = new ArrayList<>();
        obj.add(currentUserUuid);
        obj.add(recipientUuid);

        FetchNotifier credentialNotifiers = new FetchNotifier(obj, new FetchUseCases(provideChatRepository));
        compositeDisposable.add(credentialNotifiers.fetch()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> {
                            if (result.isRight()) {

                                ConversationAdapter adapter = new ConversationAdapter(
                                        result.getRight(), currentUserUuid, this);

                                LinearLayoutManager layoutManager = new LinearLayoutManager(this);
                                layoutManager.setReverseLayout(true);

                                conversationRecyclerView.setLayoutManager(layoutManager);
                                conversationRecyclerView.setAdapter(adapter);
                                adapter.startListening();

                                adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
                                    @Override
                                    public void onItemRangeInserted(int positionStart, int itemCount) {
                                        super.onItemRangeInserted(positionStart, itemCount);
                                        conversationRecyclerView.smoothScrollToPosition(0);
                                    }
                                });
                                
                            } else {
                                showSnackBar(result.getLeft().getErrorMessage());
                            }
                        },
                        throwable -> Log.e(TAG, "Error loading messages: " + throwable.getMessage())
                ));
    }

    private void sendMessage(final String message) {
        List<Object> obj = new ArrayList<>();
        obj.add(message);
        obj.add(currentUserUuid);
        obj.add(recipientUuid);

        ChatNotifier credentialNotifiers = new ChatNotifier(obj, new SendUseCases(provideChatRepository));
        compositeDisposable.add(credentialNotifiers.send()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> {
                            if (result.isRight()) {
                                composeMessageField.setText("");
                            } else {
                                showSnackBar(result.getLeft().getErrorMessage());
                            }
                        },
                        throwable -> Log.e(TAG, "Error sending messages: " + throwable.getMessage())
                ));
    }

    private void showSnackBar(String message) {
        View rootView = findViewById(android.R.id.content);
        SnackBarHelper.invoke(message, rootView);
    }
}
