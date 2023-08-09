package Scent.Danielle;

// Android components
import android.content.Intent;

// AndroidX testing components
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

// Google components
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.Task;

// Firebase components
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

// Google Sign-In components
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;

// Google Play Services components
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.tasks.Tasks;

// JUnit components
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

// Static import statements
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(AndroidJUnit4.class)
public class AuthActivityTest {

    private AuthActivity authActivity;
    private FirebaseAuth mockedFirebaseAuth;
    private GoogleSignInClient mockedGoogleSignInClient;

    @Before
    public void setUp() {
        authActivity = new AuthActivity();
        mockedFirebaseAuth = Mockito.mock(FirebaseAuth.class);
        mockedGoogleSignInClient = Mockito.mock(GoogleSignInClient.class);
        authActivity.mAuth = mockedFirebaseAuth;
        authActivity.mGoogleSignInClient = mockedGoogleSignInClient;
    }

    @Test
    public void testOnCreateWithCurrentUser() {
        when(mockedFirebaseAuth.getCurrentUser()).thenReturn(mock(FirebaseUser.class));

        ActivityScenario<AuthActivity> scenario = ActivityScenario.launch(AuthActivity.class);
        scenario.onActivity(activity -> {
            verify(mockedFirebaseAuth).getCurrentUser();
            verifyNoInteractions(mockedGoogleSignInClient);
            assertTrue(activity.isFinishing());
        });
    }

    @Test
    public void testOnCreateWithoutCurrentUser() {
        when(mockedFirebaseAuth.getCurrentUser()).thenReturn(null);

        ActivityScenario<AuthActivity> scenario = ActivityScenario.launch(AuthActivity.class);
        scenario.onActivity(activity -> {
            verify(mockedFirebaseAuth).getCurrentUser();
            verify(mockedGoogleSignInClient).getSignInIntent();
            assertNotNull(activity.signInButton);
            assertFalse(activity.isFinishing());
        });
    }

    @Test
    public void testStartHomeActivity() {
        ActivityScenario<AuthActivity> scenario = ActivityScenario.launch(AuthActivity.class);
        scenario.onActivity(activity -> {
            Intent expectedIntent = new Intent(activity, HomeActivity.class);
            Intent actualIntent = shadowOf(activity).getNextStartedActivity();
            assertNull(actualIntent);

            activity.startHomeActivity();

            actualIntent = shadowOf(activity).getNextStartedActivity();
            assertNotNull(actualIntent);
            assertEquals(expectedIntent.getComponent(), actualIntent.getComponent());
        });
    }

    @Test
    public void testHandleGoogleSignInResult_Success() throws Exception {
        GoogleSignInAccount mockSignInAccount = Mockito.mock(GoogleSignInAccount.class);
        Task<GoogleSignInAccount> mockTask = Tasks.forResult(mockSignInAccount);

        authActivity.handleGoogleSignInResult(mockTask);

        verify(mockSignInAccount).getIdToken();
        verify(mockedFirebaseAuth).signInWithCredential(any());
        verify(authActivity, times(1)).firebaseAuthWithGoogle(mockSignInAccount);
    }

    @Test
    public void testHandleGoogleSignInResult_Failure() throws Exception {
        int statusCode = ConnectionResult.API_UNAVAILABLE;
        Status status = new Status(statusCode);
        ApiException mockApiException = new ApiException(status);
        Task<GoogleSignInAccount> mockTask = Tasks.forException(mockApiException);

        authActivity.handleGoogleSignInResult(mockTask);

        verify(mockedFirebaseAuth, never()).signInWithCredential(any());
        verify(authActivity, times(1)).handleAuthenticationFailure(mockApiException);
    }

    @Test
    public void testFirebaseAuthWithGoogle_Success() {
        String mockIdToken = "mockIdToken";
        GoogleSignInAccount mockSignInAccount = Mockito.mock(GoogleSignInAccount.class);
        AuthCredential mockCredential = Mockito.mock(AuthCredential.class);
        when(mockSignInAccount.getIdToken()).thenReturn(mockIdToken);
        when(GoogleAuthProvider.getCredential(mockIdToken, null)).thenReturn(mockCredential);
        when(mockedFirebaseAuth.signInWithCredential(mockCredential)).thenReturn(Tasks.forResult(null));

        authActivity.firebaseAuthWithGoogle(mockSignInAccount);

        verify(mockSignInAccount).getIdToken();
        verify(mockedFirebaseAuth).signInWithCredential(mockCredential);
        verify(authActivity, times(1)).handleAuthenticationSuccess(null);
    }

    @Test
    public void testFirebaseAuthWithGoogle_Failure() {
        String mockIdToken = "mockIdToken";
        GoogleSignInAccount mockSignInAccount = Mockito.mock(GoogleSignInAccount.class);
        AuthCredential mockCredential = Mockito.mock(AuthCredential.class);
        when(mockSignInAccount.getIdToken()).thenReturn(mockIdToken);
        when(GoogleAuthProvider.getCredential(mockIdToken, null)).thenReturn(mockCredential);
        ApiException mockApiException = Mockito.mock(ApiException.class);
        when(mockedFirebaseAuth.signInWithCredential(mockCredential)).thenReturn(Tasks.forException(mockApiException));

        authActivity.firebaseAuthWithGoogle(mockSignInAccount);

        verify(mockSignInAccount).getIdToken();
        verify(mockedFirebaseAuth).signInWithCredential(mockCredential);
        verify(authActivity, times(1)).handleAuthenticationFailure(mockApiException);
    }
}