package com.tommihirvonen.exifnotes.activities;

import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.filters.LargeTest;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.runner.AndroidJUnit4;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.NumberPicker;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;

import com.tommihirvonen.exifnotes.R;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.is;

/**
 * Prerequisites: The database has to be empty, permissions have to be granted and the EULA agreed.
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class AddGearTest {

    private static void pauseTestFor(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void setNumberPickerValue(int pickerId, final int value) {
        onView(withId(pickerId)).perform(new ViewAction() {
            @Override
            public Matcher getConstraints() {
                return ViewMatchers.isAssignableFrom(NumberPicker.class);
            }

            @Override
            public String getDescription() {
                return "Set the value of a NumberPicker";
            }

            @Override
            public void perform(UiController uiController, View view) {
                ((NumberPicker)view).setValue(value);
            }
        });
    }


    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<>(MainActivity.class);

    @Rule
    public GrantPermissionRule mGrantPermissionRule =
            GrantPermissionRule.grant(
                    "android.permission.ACCESS_FINE_LOCATION",
                    "android.permission.ACCESS_COARSE_LOCATION",
                    "android.permission.WRITE_EXTERNAL_STORAGE");

    @Test
    public void addGearTest() {

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





        // Add cameras

        onView(
                allOf(withContentDescription("CAMERAS"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.sliding_tabs),
                                        0),
                                0),
                        isDisplayed())).perform(click());

        // Camera 1
        onView(
                allOf(withId(R.id.fab_cameras),
                        childAtPosition(
                                withParent(withId(R.id.viewpager)),
                                2),
                        isDisplayed())).perform(click());

        pauseTestFor(100);

        onView(
                allOf(withId(R.id.make_editText),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        0),
                                1),
                        isDisplayed())).perform(replaceText("Canon"), closeSoftKeyboard());

        onView(
                allOf(withId(R.id.model_editText),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        1),
                                2),
                        isDisplayed())).perform(replaceText("A-1"), closeSoftKeyboard());

        onView(
                allOf(withId(R.id.serialNumber_editText),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        2),
                                2),
                        isDisplayed())).perform(replaceText("123456ABC"), closeSoftKeyboard());

        onView(
                allOf(withId(R.id.serialNumber_editText), withText("123456ABC"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        2),
                                2),
                        isDisplayed())).perform(pressImeActionButton());

        onView(
                allOf(withId(R.id.increment_layout),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.nested_scroll_view),
                                        0),
                                3),
                        isDisplayed())).perform(click());

        onData(anything())
                .inAdapterView(allOf(withClassName(is("com.android.internal.app.AlertController$RecycleListView")),
                        childAtPosition(
                                withClassName(is("android.widget.FrameLayout")),
                                0)))
                .atPosition(1).perform(click());

       onView(
                allOf(withId(R.id.shutter_range_layout),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.nested_scroll_view),
                                        0),
                                4),
                        isDisplayed())).perform(click());

        setNumberPickerValue(R.id.number_picker_one, 6);
        setNumberPickerValue(R.id.number_picker_two, 36);

        onView(
                allOf(withId(android.R.id.button1), withText("OK"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.ScrollView")),
                                        0),
                                3))).perform(scrollTo(), click());

        // Check the text in the shutter speed range TextView is correct
        onView(allOf(withId(R.id.shutter_range_text), isDisplayed()))
                .check(matches(withText("1/1000 - 30\"")));

        onView(
                allOf(withId(R.id.exposure_comp_increment_layout),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.nested_scroll_view),
                                        0),
                                5),
                        isDisplayed())).perform(click());

        onData(anything())
                .inAdapterView(allOf(withClassName(is("com.android.internal.app.AlertController$RecycleListView")),
                        childAtPosition(
                                withClassName(is("android.widget.FrameLayout")),
                                0)))
                .atPosition(1).perform(click());

        onView(
                allOf(withId(android.R.id.button1), withText("Add"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.ScrollView")),
                                        0),
                                3))).perform(scrollTo(), click());


        // Camera 2

        onView(
                allOf(withId(R.id.fab_cameras),
                        childAtPosition(
                                withParent(withId(R.id.viewpager)),
                                2),
                        isDisplayed())).perform(click());

        onView(
                allOf(withId(R.id.make_editText),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        0),
                                1),
                        isDisplayed())).perform(replaceText("Nikon"), closeSoftKeyboard());

        onView(
                allOf(withId(R.id.model_editText),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        1),
                                2),
                        isDisplayed())).perform(replaceText("FM2"), closeSoftKeyboard());

        onView(
                allOf(withId(R.id.serialNumber_editText),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        2),
                                2),
                        isDisplayed())).perform(replaceText("CBA321"), closeSoftKeyboard());

        onView(
                allOf(withId(R.id.serialNumber_editText), withText("CBA321"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        2),
                                2),
                        isDisplayed())).perform(pressImeActionButton());

        onView(
                allOf(withId(R.id.increment_layout),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.nested_scroll_view),
                                        0),
                                3),
                        isDisplayed())).perform(click());

        onData(anything())
                .inAdapterView(allOf(withClassName(is("com.android.internal.app.AlertController$RecycleListView")),
                        childAtPosition(
                                withClassName(is("android.widget.FrameLayout")),
                                0)))
                .atPosition(1).perform(click());


        onView(
                allOf(withId(R.id.shutter_range_layout),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.nested_scroll_view),
                                        0),
                                4),
                        isDisplayed())).perform(click());

        setNumberPickerValue(R.id.number_picker_one, 6);
        setNumberPickerValue(R.id.number_picker_two, 26);

        onView(
                allOf(withId(android.R.id.button1), withText("OK"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.ScrollView")),
                                        0),
                                3))).perform(scrollTo(), click());

        // Check the text in the shutter speed range TextView is correct
        onView(allOf(withId(R.id.shutter_range_text), isDisplayed()))
                .check(matches(withText("1/1000 - 1\"")));

        onView(
                allOf(withId(android.R.id.button1), withText("Add"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.ScrollView")),
                                        0),
                                3))).perform(scrollTo(), click());





        // Add lenses

        onView(
                allOf(withContentDescription("LENSES"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.sliding_tabs),
                                        0),
                                1),
                        isDisplayed())).perform(click());

        // Lens 1
        onView(
                allOf(withId(R.id.fab_lenses),
                        childAtPosition(
                                withParent(withId(R.id.viewpager)),
                                2),
                        isDisplayed())).perform(click());

        onView(
                allOf(withId(R.id.make_editText),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        0),
                                1),
                        isDisplayed())).perform(replaceText("Canon"), closeSoftKeyboard());

        onView(
                allOf(withId(R.id.model_editText),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        1),
                                2),
                        isDisplayed())).perform(replaceText("FD 28mm f/2.8"), closeSoftKeyboard());

        onView(
                allOf(withId(R.id.increment_layout),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.nested_scroll_view),
                                        0),
                                3),
                        isDisplayed())).perform(click());

        onData(anything())
                .inAdapterView(allOf(withClassName(is("com.android.internal.app.AlertController$RecycleListView")),
                        childAtPosition(
                                withClassName(is("android.widget.FrameLayout")),
                                0)))
                .atPosition(1).perform(click());

        onView(
                allOf(withId(R.id.serialNumber_editText),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        2),
                                2),
                        isDisplayed())).perform(pressImeActionButton());

        onView(
                allOf(withId(R.id.aperture_range_layout),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.nested_scroll_view),
                                        0),
                                4),
                        isDisplayed())).perform(click());

        setNumberPickerValue(R.id.number_picker_one, 17);
        setNumberPickerValue(R.id.number_picker_two, 5);

        onView(
                allOf(withId(android.R.id.button1), withText("OK"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.ScrollView")),
                                        0),
                                3))).perform(scrollTo(), click());

        // Check the text in the aperture range TextView
        onView(allOf(withId(R.id.aperture_range_text), isDisplayed()))
                .check(matches(withText("f/2.8 - f/22")));

        onView(
                allOf(withId(R.id.focal_length_range_layout),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.nested_scroll_view),
                                        0),
                                5),
                        isDisplayed())).perform(click());

        setNumberPickerValue(R.id.number_picker_one, 28);
        setNumberPickerValue(R.id.number_picker_two, 28);

        onView(
                allOf(withId(android.R.id.button1), withText("OK"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.ScrollView")),
                                        0),
                                3))).perform(scrollTo(), click());

        onView(allOf(withId(R.id.focal_length_range_text), isDisplayed()))
                .check(matches(withText("28")));

        onView(
                allOf(withId(android.R.id.button1), withText("Add"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.ScrollView")),
                                        0),
                                3))).perform(scrollTo(), click());


        // Lens 2

        onView(
                allOf(withId(R.id.fab_lenses),
                        childAtPosition(
                                withParent(withId(R.id.viewpager)),
                                2),
                        isDisplayed())).perform(click());

        onView(
                allOf(withId(R.id.make_editText),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        0),
                                1),
                        isDisplayed())).perform(replaceText("Canon"), closeSoftKeyboard());

        onView(
                allOf(withId(R.id.model_editText),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        1),
                                2),
                        isDisplayed())).perform(replaceText("FD 50mm f/1.8"), closeSoftKeyboard());

        onView(
                allOf(withId(R.id.model_editText), withText("FD 50mm f/1.8"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        1),
                                2),
                        isDisplayed())).perform(pressImeActionButton());

        onView(
                allOf(withId(R.id.serialNumber_editText),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        2),
                                2),
                        isDisplayed())).perform(pressImeActionButton());

        onView(
                allOf(withId(R.id.increment_layout),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.nested_scroll_view),
                                        0),
                                3),
                        isDisplayed())).perform(click());

        onData(anything())
                .inAdapterView(allOf(withClassName(is("com.android.internal.app.AlertController$RecycleListView")),
                        childAtPosition(
                                withClassName(is("android.widget.FrameLayout")),
                                0)))
                .atPosition(1).perform(click());

        onView(
                allOf(withId(R.id.aperture_range_layout),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.nested_scroll_view),
                                        0),
                                4),
                        isDisplayed())).perform(click());

        setNumberPickerValue(R.id.number_picker_one, 20);
        setNumberPickerValue(R.id.number_picker_two, 5);

        onView(
                allOf(withId(android.R.id.button1), withText("OK"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.ScrollView")),
                                        0),
                                3))).perform(scrollTo(), click());

        // Check the text in the aperture range TextView
        onView(allOf(withId(R.id.aperture_range_text), isDisplayed()))
                .check(matches(withText("f/1.8 - f/22")));

        onView(
                allOf(withId(R.id.focal_length_range_layout),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.nested_scroll_view),
                                        0),
                                5),
                        isDisplayed())).perform(click());

        onView(
                allOf(withId(R.id.picker_one_fast_forward),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        0),
                                2),
                        isDisplayed())).perform(click());

        onView(
                allOf(withId(R.id.picker_two_fast_forward),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        2),
                                2),
                        isDisplayed())).perform(click());

        onView(
                allOf(withId(android.R.id.button1), withText("OK"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.ScrollView")),
                                        0),
                                3))).perform(scrollTo(), click());

        onView(allOf(withId(R.id.focal_length_range_text), isDisplayed()))
                .check(matches(withText("50")));

        onView(
                allOf(withId(android.R.id.button1), withText("Add"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.ScrollView")),
                                        0),
                                3))).perform(scrollTo(), click());

        // Lens 3

        onView(
                allOf(withId(R.id.fab_lenses),
                        childAtPosition(
                                withParent(withId(R.id.viewpager)),
                                2),
                        isDisplayed())).perform(click());

        onView(
                allOf(withId(R.id.make_editText),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        0),
                                1),
                        isDisplayed())).perform(replaceText("Nikon"), closeSoftKeyboard());

        onView(
                allOf(withId(R.id.model_editText),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        1),
                                2),
                        isDisplayed())).perform(replaceText("28mm /2.8 AI"), closeSoftKeyboard());

        onView(
                allOf(withId(R.id.serialNumber_editText),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        2),
                                2),
                        isDisplayed())).perform(pressImeActionButton());

        onView(
                allOf(withId(R.id.increment_layout),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.nested_scroll_view),
                                        0),
                                3),
                        isDisplayed())).perform(click());

        onData(anything())
                .inAdapterView(allOf(withClassName(is("com.android.internal.app.AlertController$RecycleListView")),
                        childAtPosition(
                                withClassName(is("android.widget.FrameLayout")),
                                0)))
                .atPosition(2).perform(click());

        onView(
                allOf(withId(R.id.aperture_range_layout),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.nested_scroll_view),
                                        0),
                                4),
                        isDisplayed())).perform(click());

        setNumberPickerValue(R.id.number_picker_one, 9);
        setNumberPickerValue(R.id.number_picker_two, 3);

        onView(
                allOf(withId(android.R.id.button1), withText("OK"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.ScrollView")),
                                        0),
                                3))).perform(scrollTo(), click());

        // Check the text in the aperture range TextView
        onView(allOf(withId(R.id.aperture_range_text), isDisplayed()))
                .check(matches(withText("f/2.8 - f/22")));

        onView(
                allOf(withId(R.id.focal_length_range_layout),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.nested_scroll_view),
                                        0),
                                5),
                        isDisplayed())).perform(click());

        setNumberPickerValue(R.id.number_picker_one, 28);
        setNumberPickerValue(R.id.number_picker_two, 28);

        onView(
                allOf(withId(android.R.id.button1), withText("OK"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.ScrollView")),
                                        0),
                                3))).perform(scrollTo(), click());

        onView(allOf(withId(R.id.focal_length_range_text), isDisplayed()))
                .check(matches(withText("28")));

        onView(
                allOf(withId(android.R.id.button1), withText("Add"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.ScrollView")),
                                        0),
                                3))).perform(scrollTo(), click());


        // Lens 4

        onView(
                allOf(withId(R.id.fab_lenses),
                        childAtPosition(
                                withParent(withId(R.id.viewpager)),
                                2),
                        isDisplayed())).perform(click());

        onView(
                allOf(withId(R.id.make_editText),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        0),
                                1),
                        isDisplayed())).perform(replaceText("Canon"), closeSoftKeyboard());

        onView(
                allOf(withId(R.id.model_editText),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        1),
                                2),
                        isDisplayed())).perform(replaceText("FD 35-70mm f/4"), closeSoftKeyboard());

        onView(
                allOf(withId(R.id.serialNumber_editText),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        2),
                                2),
                        isDisplayed())).perform(pressImeActionButton());

        onView(
                allOf(withId(R.id.increment_layout),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.nested_scroll_view),
                                        0),
                                3),
                        isDisplayed())).perform(click());

        onData(anything())
                .inAdapterView(allOf(withClassName(is("com.android.internal.app.AlertController$RecycleListView")),
                        childAtPosition(
                                withClassName(is("android.widget.FrameLayout")),
                                0)))
                .atPosition(1).perform(click());

        onView(
                allOf(withId(R.id.aperture_range_layout),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.nested_scroll_view),
                                        0),
                                4),
                        isDisplayed())).perform(click());

        setNumberPickerValue(R.id.number_picker_one, 15);
        setNumberPickerValue(R.id.number_picker_two, 5);

        onView(
                allOf(withId(android.R.id.button1), withText("OK"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.ScrollView")),
                                        0),
                                3))).perform(scrollTo(), click());

        // Check the text in the aperture range TextView
        onView(allOf(withId(R.id.aperture_range_text), isDisplayed()))
                .check(matches(withText("f/4.0 - f/22")));

        onView(
                allOf(withId(R.id.focal_length_range_layout),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.nested_scroll_view),
                                        0),
                                5),
                        isDisplayed())).perform(click());

        setNumberPickerValue(R.id.number_picker_one, 35);
        setNumberPickerValue(R.id.number_picker_two, 70);

        onView(
                allOf(withId(android.R.id.button1), withText("OK"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.ScrollView")),
                                        0),
                                3))).perform(scrollTo(), click());

        onView(allOf(withId(R.id.focal_length_range_text), isDisplayed()))
                .check(matches(withText("35 - 70")));

        onView(
                allOf(withId(android.R.id.button1), withText("Add"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.ScrollView")),
                                        0),
                                3))).perform(scrollTo(), click());




        // Select mountable cameras for added lenses

        // Lens 1

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
                allOf(withId(android.R.id.title), withText("Select mountable cameras"),
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

        onView(
                allOf(withId(android.R.id.button1), withText("OK"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.buttonPanel),
                                        0),
                                3))).perform(scrollTo(), click());

        // Lens 2

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
                allOf(withId(android.R.id.title), withText("Select mountable cameras"),
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

        onView(
                allOf(withId(android.R.id.button1), withText("OK"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.buttonPanel),
                                        0),
                                3))).perform(scrollTo(), click());

        // Lens 3

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
                allOf(withId(android.R.id.title), withText("Select mountable cameras"),
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
                .atPosition(1).perform(click());

        onView(
                allOf(withId(android.R.id.button1), withText("OK"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.buttonPanel),
                                        0),
                                3))).perform(scrollTo(), click());


        // Lens 4

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
                allOf(withId(android.R.id.title), withText("Select mountable cameras"),
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

        onView(
                allOf(withId(android.R.id.button1), withText("OK"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.buttonPanel),
                                        0),
                                3))).perform(scrollTo(), click());




        // Add filters

        onView(
                allOf(withContentDescription("FILTERS"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.sliding_tabs),
                                        0),
                                2),
                        isDisplayed())).perform(click());

        // Filter 1

        onView(
                allOf(withId(R.id.fab_filters),
                        childAtPosition(
                                withParent(withId(R.id.viewpager)),
                                2),
                        isDisplayed())).perform(click());

        onView(
                allOf(withId(R.id.make_editText),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        0),
                                1),
                        isDisplayed())).perform(replaceText("Haida"), closeSoftKeyboard());

        onView(
                allOf(withId(R.id.model_editText),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        1),
                                2),
                        isDisplayed())).perform(replaceText("C-POL PRO II"), closeSoftKeyboard());

        onView(
                allOf(withId(android.R.id.button1), withText("Add"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.ScrollView")),
                                        0),
                                3))).perform(scrollTo(), click());

        // Filter 2

        onView(
                allOf(withId(R.id.fab_filters),
                        childAtPosition(
                                withParent(withId(R.id.viewpager)),
                                2),
                        isDisplayed())).perform(click());

        onView(
                allOf(withId(R.id.make_editText),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        0),
                                1),
                        isDisplayed())).perform(replaceText("Hoya"), closeSoftKeyboard());

        onView(
                allOf(withId(R.id.model_editText),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        1),
                                2),
                        isDisplayed())).perform(replaceText("ND x64"), closeSoftKeyboard());

        onView(
                allOf(withId(android.R.id.button1), withText("Add"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.ScrollView")),
                                        0),
                                3))).perform(scrollTo(), click());




        // Select mountable lenses for filters

        // Filter 1

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

        // Filter 2

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




        // Go back to main page

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
