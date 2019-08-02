package com.tommihirvonen.exifnotes.activities;

import androidx.test.espresso.DataInteraction;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.filters.LargeTest;
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
import static androidx.test.espresso.matcher.ViewMatchers.*;

import com.tommihirvonen.exifnotes.R;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.is;

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

        ViewInteraction overflowMenuButton = onView(
                allOf(withContentDescription("More options"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.action_bar),
                                        2),
                                2),
                        isDisplayed()));
        overflowMenuButton.perform(click());

        ViewInteraction appCompatTextView = onView(
                allOf(withId(R.id.title), withText("Gear"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.content),
                                        0),
                                0),
                        isDisplayed()));
        appCompatTextView.perform(click());





        // Add cameras

        ViewInteraction tabView0 = onView(
                allOf(withContentDescription("CAMERAS"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.sliding_tabs),
                                        0),
                                0),
                        isDisplayed()));
        tabView0.perform(click());

        ViewInteraction floatingActionButton = onView(
                allOf(withId(R.id.fab_cameras),
                        childAtPosition(
                                withParent(withId(R.id.viewpager)),
                                2),
                        isDisplayed()));
        floatingActionButton.perform(click());

        pauseTestFor(100);

        ViewInteraction appCompatEditText = onView(
                allOf(withId(R.id.make_editText),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        0),
                                1),
                        isDisplayed()));
        appCompatEditText.perform(replaceText("Canon"), closeSoftKeyboard());

        ViewInteraction appCompatEditText2 = onView(
                allOf(withId(R.id.model_editText),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        1),
                                2),
                        isDisplayed()));
        appCompatEditText2.perform(replaceText("A-1"), closeSoftKeyboard());

        ViewInteraction appCompatEditText3 = onView(
                allOf(withId(R.id.serialNumber_editText),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        2),
                                2),
                        isDisplayed()));
        appCompatEditText3.perform(replaceText("123456ABC"), closeSoftKeyboard());

        ViewInteraction appCompatEditText4 = onView(
                allOf(withId(R.id.serialNumber_editText), withText("123456ABC"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        2),
                                2),
                        isDisplayed()));
        appCompatEditText4.perform(pressImeActionButton());

        ViewInteraction linearLayout = onView(
                allOf(withId(R.id.increment_layout),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.nested_scroll_view),
                                        0),
                                3),
                        isDisplayed()));
        linearLayout.perform(click());

        DataInteraction appCompatCheckedTextView = onData(anything())
                .inAdapterView(allOf(withClassName(is("com.android.internal.app.AlertController$RecycleListView")),
                        childAtPosition(
                                withClassName(is("android.widget.FrameLayout")),
                                0)))
                .atPosition(1);
        appCompatCheckedTextView.perform(click());

        ViewInteraction linearLayout2 = onView(
                allOf(withId(R.id.shutter_range_layout),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.nested_scroll_view),
                                        0),
                                4),
                        isDisplayed()));
        linearLayout2.perform(click());

        setNumberPickerValue(R.id.number_picker_one, 6);
        setNumberPickerValue(R.id.number_picker_two, 36);

        ViewInteraction appCompatButton2 = onView(
                allOf(withId(android.R.id.button1), withText("OK"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.ScrollView")),
                                        0),
                                3)));
        appCompatButton2.perform(scrollTo(), click());

        ViewInteraction linearLayout3 = onView(
                allOf(withId(R.id.exposure_comp_increment_layout),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.nested_scroll_view),
                                        0),
                                5),
                        isDisplayed()));
        linearLayout3.perform(click());

        DataInteraction appCompatCheckedTextView2 = onData(anything())
                .inAdapterView(allOf(withClassName(is("com.android.internal.app.AlertController$RecycleListView")),
                        childAtPosition(
                                withClassName(is("android.widget.FrameLayout")),
                                0)))
                .atPosition(1);
        appCompatCheckedTextView2.perform(click());

        ViewInteraction appCompatButton3 = onView(
                allOf(withId(android.R.id.button1), withText("Add"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.ScrollView")),
                                        0),
                                3)));
        appCompatButton3.perform(scrollTo(), click());

        ViewInteraction floatingActionButton2 = onView(
                allOf(withId(R.id.fab_cameras),
                        childAtPosition(
                                withParent(withId(R.id.viewpager)),
                                2),
                        isDisplayed()));
        floatingActionButton2.perform(click());

        ViewInteraction appCompatEditText5 = onView(
                allOf(withId(R.id.make_editText),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        0),
                                1),
                        isDisplayed()));
        appCompatEditText5.perform(replaceText("Nikon"), closeSoftKeyboard());

        ViewInteraction appCompatEditText6 = onView(
                allOf(withId(R.id.model_editText),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        1),
                                2),
                        isDisplayed()));
        appCompatEditText6.perform(replaceText("FM2"), closeSoftKeyboard());

        ViewInteraction appCompatEditText7 = onView(
                allOf(withId(R.id.serialNumber_editText),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        2),
                                2),
                        isDisplayed()));
        appCompatEditText7.perform(replaceText("CBA321"), closeSoftKeyboard());

        ViewInteraction appCompatEditText8 = onView(
                allOf(withId(R.id.serialNumber_editText), withText("CBA321"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        2),
                                2),
                        isDisplayed()));
        appCompatEditText8.perform(pressImeActionButton());

        ViewInteraction linearLayout_ = onView(
                allOf(withId(R.id.increment_layout),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.nested_scroll_view),
                                        0),
                                3),
                        isDisplayed()));
        linearLayout_.perform(click());

        DataInteraction appCompatCheckedTextView_ = onData(anything())
                .inAdapterView(allOf(withClassName(is("com.android.internal.app.AlertController$RecycleListView")),
                        childAtPosition(
                                withClassName(is("android.widget.FrameLayout")),
                                0)))
                .atPosition(1);
        appCompatCheckedTextView_.perform(click());


        ViewInteraction linearLayout2_ = onView(
                allOf(withId(R.id.shutter_range_layout),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.nested_scroll_view),
                                        0),
                                4),
                        isDisplayed()));
        linearLayout2_.perform(click());

        setNumberPickerValue(R.id.number_picker_one, 6);
        setNumberPickerValue(R.id.number_picker_two, 36);

        ViewInteraction appCompatButton2_ = onView(
                allOf(withId(android.R.id.button1), withText("OK"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.ScrollView")),
                                        0),
                                3)));
        appCompatButton2_.perform(scrollTo(), click());

        ViewInteraction appCompatButton4 = onView(
                allOf(withId(android.R.id.button1), withText("Add"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.ScrollView")),
                                        0),
                                3)));
        appCompatButton4.perform(scrollTo(), click());





        // Add lenses

        ViewInteraction tabView = onView(
                allOf(withContentDescription("LENSES"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.sliding_tabs),
                                        0),
                                1),
                        isDisplayed()));
        tabView.perform(click());

        ViewInteraction floatingActionButton3 = onView(
                allOf(withId(R.id.fab_lenses),
                        childAtPosition(
                                withParent(withId(R.id.viewpager)),
                                2),
                        isDisplayed()));
        floatingActionButton3.perform(click());

        ViewInteraction appCompatEditText9 = onView(
                allOf(withId(R.id.make_editText),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        0),
                                1),
                        isDisplayed()));
        appCompatEditText9.perform(replaceText("Canon"), closeSoftKeyboard());

        ViewInteraction appCompatEditText10 = onView(
                allOf(withId(R.id.model_editText),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        1),
                                2),
                        isDisplayed()));
        appCompatEditText10.perform(replaceText("FD 28mm f/2.8"), closeSoftKeyboard());

        ViewInteraction linearLayout4 = onView(
                allOf(withId(R.id.increment_layout),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.nested_scroll_view),
                                        0),
                                3),
                        isDisplayed()));
        linearLayout4.perform(click());

        DataInteraction appCompatCheckedTextView3 = onData(anything())
                .inAdapterView(allOf(withClassName(is("com.android.internal.app.AlertController$RecycleListView")),
                        childAtPosition(
                                withClassName(is("android.widget.FrameLayout")),
                                0)))
                .atPosition(1);
        appCompatCheckedTextView3.perform(click());

        ViewInteraction appCompatEditText11 = onView(
                allOf(withId(R.id.serialNumber_editText),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        2),
                                2),
                        isDisplayed()));
        appCompatEditText11.perform(pressImeActionButton());

        ViewInteraction linearLayout5 = onView(
                allOf(withId(R.id.aperture_range_layout),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.nested_scroll_view),
                                        0),
                                4),
                        isDisplayed()));
        linearLayout5.perform(click());

        setNumberPickerValue(R.id.number_picker_one, 17);
        setNumberPickerValue(R.id.number_picker_two, 5);

        ViewInteraction appCompatButton5 = onView(
                allOf(withId(android.R.id.button1), withText("OK"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.ScrollView")),
                                        0),
                                3)));
        appCompatButton5.perform(scrollTo(), click());

        ViewInteraction linearLayout6 = onView(
                allOf(withId(R.id.focal_length_range_layout),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.nested_scroll_view),
                                        0),
                                5),
                        isDisplayed()));
        linearLayout6.perform(click());

        setNumberPickerValue(R.id.number_picker_one, 28);
        setNumberPickerValue(R.id.number_picker_two, 28);

        ViewInteraction appCompatButton6 = onView(
                allOf(withId(android.R.id.button1), withText("OK"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.ScrollView")),
                                        0),
                                3)));
        appCompatButton6.perform(scrollTo(), click());

        ViewInteraction appCompatButton7 = onView(
                allOf(withId(android.R.id.button1), withText("Add"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.ScrollView")),
                                        0),
                                3)));
        appCompatButton7.perform(scrollTo(), click());

        ViewInteraction floatingActionButton4 = onView(
                allOf(withId(R.id.fab_lenses),
                        childAtPosition(
                                withParent(withId(R.id.viewpager)),
                                2),
                        isDisplayed()));
        floatingActionButton4.perform(click());

        ViewInteraction appCompatEditText12 = onView(
                allOf(withId(R.id.make_editText),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        0),
                                1),
                        isDisplayed()));
        appCompatEditText12.perform(replaceText("Canon"), closeSoftKeyboard());

        ViewInteraction appCompatEditText13 = onView(
                allOf(withId(R.id.model_editText),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        1),
                                2),
                        isDisplayed()));
        appCompatEditText13.perform(replaceText("FD 50mm f/1.8"), closeSoftKeyboard());

        ViewInteraction appCompatEditText14 = onView(
                allOf(withId(R.id.model_editText), withText("FD 50mm f/1.8"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        1),
                                2),
                        isDisplayed()));
        appCompatEditText14.perform(pressImeActionButton());

        ViewInteraction appCompatEditText15 = onView(
                allOf(withId(R.id.serialNumber_editText),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        2),
                                2),
                        isDisplayed()));
        appCompatEditText15.perform(pressImeActionButton());

        ViewInteraction linearLayout9 = onView(
                allOf(withId(R.id.increment_layout),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.nested_scroll_view),
                                        0),
                                3),
                        isDisplayed()));
        linearLayout9.perform(click());

        DataInteraction appCompatCheckedTextView4 = onData(anything())
                .inAdapterView(allOf(withClassName(is("com.android.internal.app.AlertController$RecycleListView")),
                        childAtPosition(
                                withClassName(is("android.widget.FrameLayout")),
                                0)))
                .atPosition(1);
        appCompatCheckedTextView4.perform(click());

        ViewInteraction linearLayout10 = onView(
                allOf(withId(R.id.aperture_range_layout),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.nested_scroll_view),
                                        0),
                                4),
                        isDisplayed()));
        linearLayout10.perform(click());

        setNumberPickerValue(R.id.number_picker_one, 20);
        setNumberPickerValue(R.id.number_picker_two, 5);

        ViewInteraction appCompatButton8 = onView(
                allOf(withId(android.R.id.button1), withText("OK"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.ScrollView")),
                                        0),
                                3)));
        appCompatButton8.perform(scrollTo(), click());

        ViewInteraction linearLayout11 = onView(
                allOf(withId(R.id.focal_length_range_layout),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.nested_scroll_view),
                                        0),
                                5),
                        isDisplayed()));
        linearLayout11.perform(click());

        ViewInteraction linearLayout12 = onView(
                allOf(withId(R.id.picker_one_fast_forward),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        0),
                                2),
                        isDisplayed()));
        linearLayout12.perform(click());

        ViewInteraction linearLayout13 = onView(
                allOf(withId(R.id.picker_two_fast_forward),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        2),
                                2),
                        isDisplayed()));
        linearLayout13.perform(click());

        ViewInteraction appCompatButton9 = onView(
                allOf(withId(android.R.id.button1), withText("OK"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.ScrollView")),
                                        0),
                                3)));
        appCompatButton9.perform(scrollTo(), click());

        ViewInteraction appCompatButton10 = onView(
                allOf(withId(android.R.id.button1), withText("Add"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.ScrollView")),
                                        0),
                                3)));
        appCompatButton10.perform(scrollTo(), click());

        ViewInteraction floatingActionButton5 = onView(
                allOf(withId(R.id.fab_lenses),
                        childAtPosition(
                                withParent(withId(R.id.viewpager)),
                                2),
                        isDisplayed()));
        floatingActionButton5.perform(click());

        ViewInteraction appCompatEditText16 = onView(
                allOf(withId(R.id.make_editText),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        0),
                                1),
                        isDisplayed()));
        appCompatEditText16.perform(replaceText("Nikon"), closeSoftKeyboard());

        ViewInteraction appCompatEditText17 = onView(
                allOf(withId(R.id.model_editText),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        1),
                                2),
                        isDisplayed()));
        appCompatEditText17.perform(replaceText("28mm /2.8 AI"), closeSoftKeyboard());

        ViewInteraction appCompatEditText18 = onView(
                allOf(withId(R.id.serialNumber_editText),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        2),
                                2),
                        isDisplayed()));
        appCompatEditText18.perform(pressImeActionButton());

        ViewInteraction linearLayout14 = onView(
                allOf(withId(R.id.increment_layout),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.nested_scroll_view),
                                        0),
                                3),
                        isDisplayed()));
        linearLayout14.perform(click());

        DataInteraction appCompatCheckedTextView5 = onData(anything())
                .inAdapterView(allOf(withClassName(is("com.android.internal.app.AlertController$RecycleListView")),
                        childAtPosition(
                                withClassName(is("android.widget.FrameLayout")),
                                0)))
                .atPosition(2);
        appCompatCheckedTextView5.perform(click());

        ViewInteraction linearLayout15 = onView(
                allOf(withId(R.id.aperture_range_layout),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.nested_scroll_view),
                                        0),
                                4),
                        isDisplayed()));
        linearLayout15.perform(click());

        setNumberPickerValue(R.id.number_picker_one, 9);
        setNumberPickerValue(R.id.number_picker_two, 3);

        ViewInteraction appCompatButton11 = onView(
                allOf(withId(android.R.id.button1), withText("OK"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.ScrollView")),
                                        0),
                                3)));
        appCompatButton11.perform(scrollTo(), click());

        ViewInteraction linearLayout16 = onView(
                allOf(withId(R.id.focal_length_range_layout),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.nested_scroll_view),
                                        0),
                                5),
                        isDisplayed()));
        linearLayout16.perform(click());

        setNumberPickerValue(R.id.number_picker_one, 28);
        setNumberPickerValue(R.id.number_picker_two, 28);

        ViewInteraction appCompatButton12 = onView(
                allOf(withId(android.R.id.button1), withText("OK"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.ScrollView")),
                                        0),
                                3)));
        appCompatButton12.perform(scrollTo(), click());

        ViewInteraction appCompatButton13 = onView(
                allOf(withId(android.R.id.button1), withText("Add"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.ScrollView")),
                                        0),
                                3)));
        appCompatButton13.perform(scrollTo(), click());




        // Select mountable cameras for added lenses

        ViewInteraction linearLayout17 = onView(
                allOf(withId(R.id.item_gear_layout),
                        childAtPosition(
                                allOf(withId(R.id.lenses_recycler_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.FrameLayout")),
                                                1)),
                                0),
                        isDisplayed()));
        linearLayout17.perform(click());

        ViewInteraction textView = onView(
                allOf(withId(android.R.id.title), withText("Select mountable cameras"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("com.android.internal.view.menu.ListMenuItemView")),
                                        0),
                                0),
                        isDisplayed()));
        textView.perform(click());

        DataInteraction appCompatCheckedTextView6 = onData(anything())
                .inAdapterView(allOf(withId(R.id.select_dialog_listview),
                        childAtPosition(
                                withId(R.id.contentPanel),
                                0)))
                .atPosition(0);
        appCompatCheckedTextView6.perform(click());

        ViewInteraction appCompatButton14 = onView(
                allOf(withId(android.R.id.button1), withText("OK"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.buttonPanel),
                                        0),
                                3)));
        appCompatButton14.perform(scrollTo(), click());

        ViewInteraction linearLayout19 = onView(
                allOf(withId(R.id.item_gear_layout),
                        childAtPosition(
                                allOf(withId(R.id.lenses_recycler_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.FrameLayout")),
                                                1)),
                                1),
                        isDisplayed()));
        linearLayout19.perform(click());

        ViewInteraction textView2 = onView(
                allOf(withId(android.R.id.title), withText("Select mountable cameras"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("com.android.internal.view.menu.ListMenuItemView")),
                                        0),
                                0),
                        isDisplayed()));
        textView2.perform(click());

        DataInteraction appCompatCheckedTextView7 = onData(anything())
                .inAdapterView(allOf(withId(R.id.select_dialog_listview),
                        childAtPosition(
                                withId(R.id.contentPanel),
                                0)))
                .atPosition(0);
        appCompatCheckedTextView7.perform(click());

        ViewInteraction appCompatButton15 = onView(
                allOf(withId(android.R.id.button1), withText("OK"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.buttonPanel),
                                        0),
                                3)));
        appCompatButton15.perform(scrollTo(), click());

        ViewInteraction linearLayout21 = onView(
                allOf(withId(R.id.item_gear_layout),
                        childAtPosition(
                                allOf(withId(R.id.lenses_recycler_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.FrameLayout")),
                                                1)),
                                2),
                        isDisplayed()));
        linearLayout21.perform(click());

        ViewInteraction textView4 = onView(
                allOf(withId(android.R.id.title), withText("Select mountable cameras"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("com.android.internal.view.menu.ListMenuItemView")),
                                        0),
                                0),
                        isDisplayed()));
        textView4.perform(click());

        DataInteraction appCompatCheckedTextView8 = onData(anything())
                .inAdapterView(allOf(withId(R.id.select_dialog_listview),
                        childAtPosition(
                                withId(R.id.contentPanel),
                                0)))
                .atPosition(1);
        appCompatCheckedTextView8.perform(click());

        ViewInteraction appCompatButton17 = onView(
                allOf(withId(android.R.id.button1), withText("OK"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.buttonPanel),
                                        0),
                                3)));
        appCompatButton17.perform(scrollTo(), click());




        // Add filters

        ViewInteraction tabView2 = onView(
                allOf(withContentDescription("FILTERS"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.sliding_tabs),
                                        0),
                                2),
                        isDisplayed()));
        tabView2.perform(click());

        ViewInteraction floatingActionButton6 = onView(
                allOf(withId(R.id.fab_filters),
                        childAtPosition(
                                withParent(withId(R.id.viewpager)),
                                2),
                        isDisplayed()));
        floatingActionButton6.perform(click());

        ViewInteraction appCompatEditText19 = onView(
                allOf(withId(R.id.make_editText),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        0),
                                1),
                        isDisplayed()));
        appCompatEditText19.perform(replaceText("Haida"), closeSoftKeyboard());

        ViewInteraction appCompatEditText20 = onView(
                allOf(withId(R.id.model_editText),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        1),
                                2),
                        isDisplayed()));
        appCompatEditText20.perform(replaceText("C-POL PRO II"), closeSoftKeyboard());

        ViewInteraction appCompatButton18 = onView(
                allOf(withId(android.R.id.button1), withText("Add"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.ScrollView")),
                                        0),
                                3)));
        appCompatButton18.perform(scrollTo(), click());

        ViewInteraction floatingActionButton7 = onView(
                allOf(withId(R.id.fab_filters),
                        childAtPosition(
                                withParent(withId(R.id.viewpager)),
                                2),
                        isDisplayed()));
        floatingActionButton7.perform(click());

        ViewInteraction appCompatEditText21 = onView(
                allOf(withId(R.id.make_editText),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        0),
                                1),
                        isDisplayed()));
        appCompatEditText21.perform(replaceText("Hoya"), closeSoftKeyboard());

        ViewInteraction appCompatEditText22 = onView(
                allOf(withId(R.id.model_editText),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        1),
                                2),
                        isDisplayed()));
        appCompatEditText22.perform(replaceText("ND x64"), closeSoftKeyboard());

        ViewInteraction appCompatButton19 = onView(
                allOf(withId(android.R.id.button1), withText("Add"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.ScrollView")),
                                        0),
                                3)));
        appCompatButton19.perform(scrollTo(), click());




        // Select mountable lenses for filters

        ViewInteraction linearLayout25 = onView(
                allOf(withId(R.id.item_gear_layout),
                        childAtPosition(
                                allOf(withId(R.id.filters_recycler_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.FrameLayout")),
                                                1)),
                                0),
                        isDisplayed()));
        linearLayout25.perform(click());

        ViewInteraction textView5 = onView(
                allOf(withId(android.R.id.title), withText("Select mountable lenses"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("com.android.internal.view.menu.ListMenuItemView")),
                                        0),
                                0),
                        isDisplayed()));
        textView5.perform(click());

        DataInteraction appCompatCheckedTextView9 = onData(anything())
                .inAdapterView(allOf(withId(R.id.select_dialog_listview),
                        childAtPosition(
                                withId(R.id.contentPanel),
                                0)))
                .atPosition(0);
        appCompatCheckedTextView9.perform(click());

        DataInteraction appCompatCheckedTextView10 = onData(anything())
                .inAdapterView(allOf(withId(R.id.select_dialog_listview),
                        childAtPosition(
                                withId(R.id.contentPanel),
                                0)))
                .atPosition(1);
        appCompatCheckedTextView10.perform(click());

        ViewInteraction appCompatButton20 = onView(
                allOf(withId(android.R.id.button1), withText("OK"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.buttonPanel),
                                        0),
                                3)));
        appCompatButton20.perform(scrollTo(), click());

        ViewInteraction linearLayout27 = onView(
                allOf(withId(R.id.item_gear_layout),
                        childAtPosition(
                                allOf(withId(R.id.filters_recycler_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.FrameLayout")),
                                                1)),
                                1),
                        isDisplayed()));
        linearLayout27.perform(click());

        ViewInteraction textView6 = onView(
                allOf(withId(android.R.id.title), withText("Select mountable lenses"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("com.android.internal.view.menu.ListMenuItemView")),
                                        0),
                                0),
                        isDisplayed()));
        textView6.perform(click());

        DataInteraction appCompatCheckedTextView11 = onData(anything())
                .inAdapterView(allOf(withId(R.id.select_dialog_listview),
                        childAtPosition(
                                withId(R.id.contentPanel),
                                0)))
                .atPosition(0);
        appCompatCheckedTextView11.perform(click());

        DataInteraction appCompatCheckedTextView12 = onData(anything())
                .inAdapterView(allOf(withId(R.id.select_dialog_listview),
                        childAtPosition(
                                withId(R.id.contentPanel),
                                0)))
                .atPosition(1);
        appCompatCheckedTextView12.perform(click());

        ViewInteraction appCompatButton21 = onView(
                allOf(withId(android.R.id.button1), withText("OK"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.buttonPanel),
                                        0),
                                3)));
        appCompatButton21.perform(scrollTo(), click());

        ViewInteraction appCompatImageButton = onView(
                allOf(withContentDescription("Navigate up"),
                        childAtPosition(
                                allOf(withId(R.id.action_bar),
                                        childAtPosition(
                                                withId(R.id.action_bar_container),
                                                0)),
                                1),
                        isDisplayed()));
        appCompatImageButton.perform(click());
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
