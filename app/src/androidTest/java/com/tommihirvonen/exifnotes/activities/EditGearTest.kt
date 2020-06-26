package com.tommihirvonen.exifnotes.activities

import android.view.View
import android.view.ViewGroup
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.filters.LargeTest
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.tommihirvonen.exifnotes.R
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.*
import org.hamcrest.TypeSafeMatcher
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test prerequisites: AddGearTest.java has to be successfully run first on an empty database.
 * Edits various pieces of gear.
 */
@LargeTest
@RunWith(AndroidJUnit4ClassRunner::class)
class EditGearTest {

    @Suppress("SameParameterValue")
    private fun pauseTestFor(milliseconds: Long) {
        try {
            Thread.sleep(milliseconds)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    private fun childAtPosition(
            parentMatcher: Matcher<View>, position: Int): Matcher<View> {
        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("Child at position $position in parent ")
                parentMatcher.describeTo(description)
            }

            public override fun matchesSafely(view: View): Boolean {
                val parent = view.parent
                return (parent is ViewGroup && parentMatcher.matches(parent)
                        && view == parent.getChildAt(position))
            }
        }
    }

    @Test
    fun editGearTest() {

        launchActivity<MainActivity>()

        pauseTestFor(200)

        // Navigate to GearActivity
        onView(
                allOf(withContentDescription("More options"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.action_bar),
                                        2),
                                2),
                        isDisplayed())).perform(click())
        onView(
                allOf(withId(R.id.title), withText("Gear"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.content),
                                        0),
                                0),
                        isDisplayed())).perform(click())


        // Navigate to cameras tab
        onView(
                allOf(withContentDescription("CAMERAS"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.sliding_tabs),
                                        0),
                                0),
                        isDisplayed())).perform(click())


        // Edit camera 2
        onView(
                allOf(withId(R.id.item_gear_layout),
                        childAtPosition(
                                allOf(withId(R.id.cameras_recycler_view),
                                        childAtPosition(
                                                withClassName(`is`("android.widget.FrameLayout")),
                                                1)),
                                1),
                        isDisplayed())).perform(click())
        onView(
                allOf(withId(android.R.id.title), withText("Edit"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(`is`("com.android.internal.view.menu.ListMenuItemView")),
                                        0),
                                0),
                        isDisplayed())).perform(click())
        onView(
                allOf(withId(android.R.id.button1), withText("OK"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(`is`("android.widget.ScrollView")),
                                        0),
                                3))).perform(scrollTo(), click())

        // Swap mountable lenses. Do this twice to get back to the original setup.
        repeat(2) {
            onView(
                    allOf(withId(R.id.item_gear_layout),
                            childAtPosition(
                                    allOf(withId(R.id.cameras_recycler_view),
                                            childAtPosition(
                                                    withClassName(`is`("android.widget.FrameLayout")),
                                                    1)),
                                    1),
                            isDisplayed())).perform(click())
            pauseTestFor(100)
            onView(
                    allOf(withId(android.R.id.title), withText("Select mountable lenses"),
                            childAtPosition(
                                    childAtPosition(
                                            withClassName(`is`("com.android.internal.view.menu.ListMenuItemView")),
                                            0),
                                    0),
                            isDisplayed())).perform(click())
            onData(anything())
                    .inAdapterView(allOf(withId(R.id.select_dialog_listview),
                            childAtPosition(
                                    withId(R.id.contentPanel),
                                    0)))
                    .atPosition(0).perform(click())
            onData(anything())
                    .inAdapterView(allOf(withId(R.id.select_dialog_listview),
                            childAtPosition(
                                    withId(R.id.contentPanel),
                                    0)))
                    .atPosition(1).perform(click())
            onView(
                    allOf(withId(android.R.id.button1), withText("OK"),
                            childAtPosition(
                                    childAtPosition(
                                            withId(R.id.buttonPanel),
                                            0),
                                    3))).perform(scrollTo(), click())
        }


        // Navigate to lenses tab
        onView(
                allOf(withContentDescription("LENSES"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.sliding_tabs),
                                        0),
                                1),
                        isDisplayed())).perform(click())

        // Edit lens 2
        onView(
                allOf(withId(R.id.item_gear_layout),
                        childAtPosition(
                                allOf(withId(R.id.lenses_recycler_view),
                                        childAtPosition(
                                                withClassName(`is`("android.widget.FrameLayout")),
                                                1)),
                                1),
                        isDisplayed())).perform(click())
        onView(
                allOf(withId(android.R.id.title), withText("Edit"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(`is`("com.android.internal.view.menu.ListMenuItemView")),
                                        0),
                                0),
                        isDisplayed())).perform(click())
        onView(
                allOf(withId(android.R.id.button1), withText("OK"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(`is`("android.widget.ScrollView")),
                                        0),
                                3))).perform(scrollTo(), click())

        // Set mountable filters using the lens tab
        onView(
                allOf(withId(R.id.item_gear_layout),
                        childAtPosition(
                                allOf(withId(R.id.lenses_recycler_view),
                                        childAtPosition(
                                                withClassName(`is`("android.widget.FrameLayout")),
                                                1)),
                                3),
                        isDisplayed())).perform(click())
        onView(
                allOf(withId(android.R.id.title), withText("Select mountable filters"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(`is`("com.android.internal.view.menu.ListMenuItemView")),
                                        0),
                                0),
                        isDisplayed())).perform(click())
        onData(anything())
                .inAdapterView(allOf(withId(R.id.select_dialog_listview),
                        childAtPosition(
                                withId(R.id.contentPanel),
                                0)))
                .atPosition(0).perform(click())
        onData(anything())
                .inAdapterView(allOf(withId(R.id.select_dialog_listview),
                        childAtPosition(
                                withId(R.id.contentPanel),
                                0)))
                .atPosition(1).perform(click())
        onView(
                allOf(withId(android.R.id.button1), withText("OK"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.buttonPanel),
                                        0),
                                3))).perform(scrollTo(), click())


        // Navigate to filters tab
        onView(
                allOf(withContentDescription("FILTERS"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.sliding_tabs),
                                        0),
                                2),
                        isDisplayed())).perform(click())

        // Edit filter 2
        onView(
                allOf(withId(R.id.item_gear_layout),
                        childAtPosition(
                                allOf(withId(R.id.filters_recycler_view),
                                        childAtPosition(
                                                withClassName(`is`("android.widget.FrameLayout")),
                                                1)),
                                1),
                        isDisplayed())).perform(click())
        onView(
                allOf(withId(android.R.id.title), withText("Edit"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(`is`("com.android.internal.view.menu.ListMenuItemView")),
                                        0),
                                0),
                        isDisplayed())).perform(click())
        onView(
                allOf(withId(android.R.id.button1), withText("OK"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(`is`("android.widget.ScrollView")),
                                        0),
                                3))).perform(scrollTo(), click())


        // Go back to MainActivity
        onView(
                allOf(withContentDescription("Navigate up"),
                        childAtPosition(
                                allOf(withId(R.id.action_bar),
                                        childAtPosition(
                                                withId(R.id.action_bar_container),
                                                0)),
                                1),
                        isDisplayed())).perform(click())
    }
    
}