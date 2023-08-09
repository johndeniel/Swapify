package Scent.Danielle;

// Import Android components
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

// Import androidx components
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

// Import Firebase components for authentication
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.GoogleAuthProvider;

// Import Google Sign-In components
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

// Import Google API client and connection-related components
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

public class AuthActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {

    // Constants variables
    public static final String TAG = AuthActivity.class.getSimpleName(); // Tag for logging and debugging purposes
    public static final int RC_SIGN_IN = 9001; // Request code for the Google Sign-In activity result

    // Member variables
    public FirebaseAuth mAuth; // Firebase Authentication instance
    public GoogleSignInClient mGoogleSignInClient; // Google Sign-In client
    public Button signInButton; // Button for initiating Google Sign-In

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        mAuth = FirebaseAuth.getInstance(); // Initialize Firebase Authentication
        FirebaseUser currentUser = mAuth.getCurrentUser();

        // Check if a user is already authenticated; if yes, navigate to HomeActivity
        if (currentUser != null) {
            startHomeActivity();
        } else {
            initializeGoogleSignIn(); // Initialize Google Sign-In options

            signInButton = findViewById(R.id.google_login_button);
            signInButton.setOnClickListener(v -> signIn()); // Set click listener for Google Sign-In button
        }
    }

    public void startHomeActivity() {
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
        finish(); // Close the current activity
    }

    public void initializeGoogleSignIn() {
        // Configure Google Sign-In with necessary parameters
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // Request ID token for authentication
                .requestEmail() // Request user's email
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso); // Initialize Google Sign-In client
    }

    // Initiate the Google Sign-In process
    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN); // Start the Google Sign-In activity
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Check the result of the Google Sign-In activity
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleGoogleSignInResult(task);
        } else {
            Log.w(TAG, "Unknown request code: " + requestCode);
        }
    }

    // Handle the result of Google Sign-In attempt
    public void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            firebaseAuthWithGoogle(account);
        } catch (ApiException e) {
            Log.w(TAG, "Google Sign-In failed: " + e.getStatusCode());
        }
    }

    public void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        handleAuthenticationSuccess(user);
                    } else {
                        handleAuthenticationFailure(task.getException());
                    }
                });
    }

    public void handleAuthenticationSuccess(FirebaseUser user) {
        Toast.makeText(this, "Authentication successful.", Toast.LENGTH_SHORT).show();
        startHomeActivity(); // Start HomeActivity after successful authentication
    }

    public void handleAuthenticationFailure(Exception exception) {
        Log.e(TAG, "Authentication failed: " + exception.getMessage(), exception);
        Toast.makeText(this, "Authentication failed: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e(TAG, "Google Play Services connection failed: " + connectionResult.getErrorMessage());
        Toast.makeText(this, "Google Play Services error. Please try again later.", Toast.LENGTH_SHORT).show();
    }

    // Sign out from Google when the activity is destroyed
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mGoogleSignInClient != null) {
            mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "Google Sign-Out successful");
                } else {
                    Log.e(TAG, "Error during Google Sign-Out: " + task.getException());
                }
            });
        }
    }
}