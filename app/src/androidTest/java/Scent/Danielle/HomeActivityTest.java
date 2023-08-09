package Scent.Danielle;

// Espresso
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

// JUnit and AndroidJUnitRunner
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

// JUnit annotations and AndroidJUnitRunner
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4ClassRunner.class)
public class HomeActivityTest {

    @Rule
    public ActivityScenarioRule<HomeActivity> activityScenarioRule = new ActivityScenarioRule<>(HomeActivity.class);

    @Test
    public void testFavoriteIconPress() {
        onView(withId(R.id.favorite)).perform(click());
        // Add assertions to verify the expected behavior after clicking the favorite icon.
    }

    @Test
    public void testSearchIconPress() {
        onView(withId(R.id.search)).perform(click());
        // Add assertions to verify the expected behavior after clicking the search icon.
    }

    @Test
    public void testMoreItemPress() {
        onView(withId(R.id.more)).perform(click());
        // Add assertions to verify the expected behavior after clicking the more item.
    }
}
