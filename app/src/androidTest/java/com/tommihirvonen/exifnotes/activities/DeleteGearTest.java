package com.tommihirvonen.exifnotes.activities;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.test.filters.LargeTest;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.runner.AndroidJUnit4;

import com.tommihirvonen.exifnotes.R;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

/**
 * Prerequisites: AddGearTest.java has to be successfully run first on an empty database.
 * Deletes all gear added by AddGearTest.
 */
@LargeTest
@RunWith(AndroidJUnit4ClassRunner.class)
public class DeleteGearTest {

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<>(MainActivity.class);

    @Rule
    public GrantPermissionRule mGrantPermissionRule =
            GrantPermissionRule.grant(
                    "android.permission.ACCESS_FINE_LOCATION",
                    "android.permission.ACCESS_COARSE_LOCATION",
                    "android.permission.WRITE_EXTERNAL_STORAGE");

    @Test
    public void deleteGearTest() {

        // Navigate to GearActivity
        onView(
                allOf(withContentDescription("More options"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.action_bar),
                                        2),
                                2),
                        isDisplayed())).perform(click());
        onView(
                allOf(withId(R.id.title), withText("Gear"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.content),
                                        0),
                                0),
                        isDisplayed())).perform(click());


        // Navigate to filters tab
        onView(
                allOf(withContentDescription("FILTERS"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.sliding_tabs),
                                        0),
                                2),
                        isDisplayed())).perform(click());


        // Delete filter 2
        onView(
                allOf(withId(R.id.item_gear_layout),
                        childAtPosition(
                                allOf(withId(R.id.filters_recycler_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.FrameLayout")),
                                                1)),
                                1),
                        isDisplayed())).perform(click());
        onView(
                allOf(withId(android.R.id.title), withText("Delete"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("com.android.internal.view.menu.ListMenuItemView")),
                                        0),
                                0),
                        isDisplayed())).perform(click());
        onView(
                allOf(withId(android.R.id.button1), withText("OK"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.buttonPanel),
                                        0),
                                3))).perform(click());

        // Delete filter 1
        onView(
                allOf(withId(R.id.item_gear_layout),
                        childAtPosition(
                                allOf(withId(R.id.filters_recycler_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.FrameLayout")),
                                                1)),
                                0),
                        isDisplayed())).perform(click());
        onView(
                allOf(withId(android.R.id.title), withText("Delete"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("com.android.internal.view.menu.ListMenuItemView")),
                                        0),
                                0),
                        isDisplayed())).perform(click());
        onView(
                allOf(withId(android.R.id.button1), withText("OK"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.buttonPanel),
                                        0),
                                3))).perform(click());


        // Navigate to lenses tab
        onView(
                allOf(withContentDescription("LENSES"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.sliding_tabs),
                                        0),
                                1),
                        isDisplayed())).perform(click());


        // Delete lens 3
        onView(
                allOf(withId(R.id.item_gear_layout),
                        childAtPosition(
                                allOf(withId(R.id.lenses_recycler_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.FrameLayout")),
                                                1)),
                                2),
                        isDisplayed())).perform(click());
        onView(
                allOf(withId(android.R.id.title), withText("Delete"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("com.android.internal.view.menu.ListMenuItemView")),
                                        0),
                                0),
                        isDisplayed())).perform(click());
        onView(
                allOf(withId(android.R.id.button1), withText("OK"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.buttonPanel),
                                        0),
                                3))).perform(click());

        // Delete lens 2
        onView(
                allOf(withId(R.id.item_gear_layout),
                        childAtPosition(
                                allOf(withId(R.id.lenses_recycler_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.FrameLayout")),
                                                1)),
                                1),
                        isDisplayed())).perform(click());
        onView(
                allOf(withId(android.R.id.title), withText("Delete"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("com.android.internal.view.menu.ListMenuItemView")),
                                        0),
                                0),
                        isDisplayed())).perform(click());
        onView(
                allOf(withId(android.R.id.button1), withText("OK"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.buttonPanel),
                                        0),
                                3))).perform(click());

        // Delete lens 1
        onView(
                allOf(withId(R.id.item_gear_layout),
                        childAtPosition(
                                allOf(withId(R.id.lenses_recycler_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.FrameLayout")),
                                                1)),
                                0),
                        isDisplayed())).perform(click());
        onView(
                allOf(withId(android.R.id.title), withText("Delete"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("com.android.internal.view.menu.ListMenuItemView")),
                                        0),
                                0),
                        isDisplayed())).perform(click());
        onView(
                allOf(withId(android.R.id.button1), withText("OK"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.buttonPanel),
                                        0),
                                3))).perform(click());


        // Navigate to cameras tab
        onView(
                allOf(withContentDescription("CAMERAS"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.sliding_tabs),
                                        0),
                                0),
                        isDisplayed())).perform(click());

        // Delete camera 2
        onView(
                allOf(withId(R.id.item_gear_layout),
                        childAtPosition(
                                allOf(withId(R.id.cameras_recycler_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.FrameLayout")),
                                                1)),
                                1),
                        isDisplayed())).perform(click());
        onView(
                allOf(withId(android.R.id.title), withText("Delete"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("com.android.internal.view.menu.ListMenuItemView")),
                                        0),
                                0),
                        isDisplayed())).perform(click());
        onView(
                allOf(withId(android.R.id.button1), withText("OK"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.buttonPanel),
                                        0),
                                3))).perform(click());

        // Delete camera 1
        onView(
                allOf(withId(R.id.item_gear_layout),
                        childAtPosition(
                                allOf(withId(R.id.cameras_recycler_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.FrameLayout")),
                                                1)),
                                0),
                        isDisplayed())).perform(click());
        onView(
                allOf(withId(android.R.id.title), withText("Delete"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("com.android.internal.view.menu.ListMenuItemView")),
                                        0),
                                0),
                        isDisplayed())).perform(click());
        onView(
                allOf(withId(android.R.id.button1), withText("OK"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.buttonPanel),
                                        0),
                                3))).perform(click());


        // Go back to MainActivity
        onView(
                allOf(withContentDescription("Navigate up"),
                        childAtPosition(
                                allOf(withId(R.id.action_bar),
                                        childAtPosition(
                                                withId(R.id.action_bar_container),
                                                0)),
                                1),
                        isDisplayed())).perform(click());

    }

    private static Matcher<View> childAtPosition(
            final Matcher<View> parentMatcher, final int position) {

        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("Child at position " + position + " in parent ");
                parentMatcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                ViewParent parent = view.getParent();
                return parent instanceof ViewGroup && parentMatcher.matches(parent)
                        && view.equals(((ViewGroup) parent).getChildAt(position));
            }
        };
    }

}
