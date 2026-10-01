package com.example.translationapp;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.isDisplayed;

/**
 * Instrumented test for the main activity.
 */
@RunWith(AndroidJUnit4.class)
public class MainActivityTest {

    @Rule
    public ActivityTestRule<MainActivity> activityRule =
            new ActivityTestRule<>(MainActivity.class);

    @Test
    public void ensureSwitchExists() {
        onView(withId(R.id.translationApiSwitch)).check(matches(isDisplayed()));
    }

    @Test
    public void ensureButtonExists() {
        onView(withId(R.id.openSettingsButton)).check(matches(isDisplayed()));
    }
}
