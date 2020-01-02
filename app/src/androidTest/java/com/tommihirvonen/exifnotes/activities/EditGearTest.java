package com.tommihirvonen.exifnotes.activities;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.test.filters.LargeTest;
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

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.is;

/**
 * Prerequisites: AddGearTest.java has to be successfully run first on an empty database.
 * Edits various pieces of gear.
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class EditGearTest {

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<>(MainActivity.class);

    @Rule
    public GrantPermissionRule mGrantPermissionRule =
            GrantPermissionRule.grant(
                    "android.permission.ACCESS_FINE_LOCATION",
                    "android.permission.ACCESS_COARSE_LOCATION",
                    "android.permission.WRITE_EXTERNAL_STORAGE");

    @Test
    public void editGearTest() {

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


        // Navigate to cameras tab
        onView(
                allOf(withContentDescription("CAMERAS"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.sliding_tabs),
                                        0),
                                0),
                        isDisplayed())).perform(click());



        // Edit camera 2
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
                allOf(withId(android.R.id.title), withText("Edit"),
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
                                        withClassName(is("android.widget.ScrollView")),
                                        0),
                                3))).perform(scrollTo(), click());

        // Swap mountable lenses. Do this twice to get back to the original setup.
        int counter = 0;
        while (counter < 2) {
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
                    allOf(withId(android.R.id.title), withText("Select mountable lenses"),
                            childAtPosition(
                                    childAtPosition(
                                            withClassName(is("com.android.internal.view.menu.ListMenuItemView")),
                                            0),
                                    0),
                            isDisplayed())).perform(click());
            onData(anything())
                    .inAdapterView(allOf(withId(R.id.select_dialog_listview),
                            childAtPosition(
                                    withId(R.id.contentPanel),
                                    0)))
                    .atPosition(0).perform(click());
            onData(anything())
                    .inAdapterView(allOf(withId(R.id.select_dialog_listview),
                            childAtPosition(
                                    withId(R.id.contentPanel),
                                    0)))
                    .atPosition(1).perform(click());
            onView(
                    allOf(withId(android.R.id.button1), withText("OK"),
                            childAtPosition(
                                    childAtPosition(
                                            withId(R.id.buttonPanel),
                                            0),
                                    3))).perform(scrollTo(), click());
            counter++;
        }


        // Navigate to lenses tab
        onView(
                allOf(withContentDescription("LENSES"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.sliding_tabs),
                                        0),
                                1),
                        isDisplayed())).perform(click());

        // Edit lens 2
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
                allOf(withId(android.R.id.title), withText("Edit"),
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
                                        withClassName(is("android.widget.ScrollView")),
                                        0),
                                3))).perform(scrollTo(), click());
        
        // Set mountable filters using the lens tab
        onView(
                allOf(withId(R.id.item_gear_layout),
                        childAtPosition(
                                allOf(withId(R.id.lenses_recycler_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.FrameLayout")),
                                                1)),
                                3),
                        isDisplayed())).perform(click());
        onView(
                allOf(withId(android.R.id.title), withText("Select mountable filters"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("com.android.internal.view.menu.ListMenuItemView")),
                                        0),
                                0),
                        isDisplayed())).perform(click());
        onData(anything())
                .inAdapterView(allOf(withId(R.id.select_dialog_listview),
                        childAtPosition(
                                withId(R.id.contentPanel),
                                0)))
                .atPosition(0).perform(click());
        onData(anything())
                .inAdapterView(allOf(withId(R.id.select_dialog_listview),
                        childAtPosition(
                                withId(R.id.contentPanel),
                                0)))
                .atPosition(1).perform(click());
        onView(
                allOf(withId(android.R.id.button1), withText("OK"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.buttonPanel),
                                        0),
                                3))).perform(scrollTo(), click());
        
        
        // Navigate to filters tab
        onView(
                allOf(withContentDescription("FILTERS"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.sliding_tabs),
                                        0),
                                2),
                        isDisplayed())).perform(click());

        // Edit filter 2
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
                allOf(withId(android.R.id.title), withText("Edit"),
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
                                        withClassName(is("android.widget.ScrollView")),
                                        0),
                                3))).perform(scrollTo(), click());
        

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
            public void describeTo(final Description description) {
                description.appendText("Child at position " + position + " in parent ");
                parentMatcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(final View view) {
                final ViewParent parent = view.getParent();
                return parent instanceof ViewGroup && parentMatcher.matches(parent)
                        && view.equals(((ViewGroup) parent).getChildAt(position));
            }
        };
    }

}
