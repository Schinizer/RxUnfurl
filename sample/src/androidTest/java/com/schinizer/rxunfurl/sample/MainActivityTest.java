package com.schinizer.rxunfurl.sample;


import android.support.test.espresso.ViewInteraction;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.allOf;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class MainActivityTest {

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<>(MainActivity.class);

    @Test
    public void gifRecordingTest() throws InterruptedException {
        ViewInteraction appCompatEditText = onView(allOf(withId(R.id.editText), isDisplayed()));
        ViewInteraction floatingActionButton = onView(allOf(withId(R.id.fab), isDisplayed()));

        appCompatEditText.perform(typeText("https://github.com/Schinizer/RxUnfurl"), closeSoftKeyboard());
        floatingActionButton.perform(click());

        Thread.sleep(500);

        appCompatEditText.perform(typeText("https://www.reddit.com/r/androiddev/"), closeSoftKeyboard());
        floatingActionButton.perform(click());

        Thread.sleep(500);

        appCompatEditText.perform(typeText("http://9gag.com/"), closeSoftKeyboard());
        floatingActionButton.perform(click());

        Thread.sleep(10000);
    }

}
