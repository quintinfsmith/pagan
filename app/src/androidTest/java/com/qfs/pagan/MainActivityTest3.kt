package com.qfs.pagan


import android.view.View
import android.view.ViewGroup
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.action.ViewActions.typeTextIntoFocusedView
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withChild
import androidx.test.espresso.matcher.ViewMatchers.withClassName
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.hamcrest.BaseMatcher
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.`is`
import org.hamcrest.TypeSafeMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@LargeTest
@RunWith(AndroidJUnit4::class)
class MainActivityTest3 {

    @Rule
    @JvmField
    var mActivityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    private fun <T> nth(n: Int, matcher: Matcher<T>, displayed: Boolean = true): Matcher<T> {
        return object : BaseMatcher<T>() {
            var match_count = 0

            override fun matches(item: Any): Boolean {
                if (matcher.matches(item) && displayed == isDisplayed().matches(item)) {
                    return this.match_count++ == n
                }

                return false
            }

            override fun describeTo(description: Description) {
                description.appendText("should return first matching item")
            }
        }
    }

    private fun get(id: Int): ViewInteraction {
        return onView(nth(0, withId(id)))
    }

    private fun get(class_name: String, n: Int = 0): ViewInteraction {
        return onView(
            nth(
                n,
                withClassName(
                    `is`(class_name)
                )
            )
        )
    }

    @Test
    fun amen_break_test() {
        get(R.id.btnFrontNew).perform(click())
        get("com.qfs.pagan.LineLabelStd", 1).perform(click())
        get(R.id.btnInsertLine).perform(click()) // Kick
        get(R.id.btnInsertLine).perform(click()) // Ride
        get(R.id.btnInsertLine).perform(click()) // Snare

        get(R.id.btnChoosePercussion).perform(click())
        get(R.id.rvOptions).perform( swipeUp() )

        // Tap twice, once to stop, another to click
        onView(withChild(withText("22: Crash Cymbal 1"))).perform(click())
        onView(withChild(withText("22: Crash Cymbal 1"))).perform(click())

        get("com.qfs.pagan.LineLabelStd", 2).perform(click())
        get(R.id.btnChoosePercussion).perform(click())
        get(R.id.rvOptions).perform( swipeUp() )

        // Tap twice, once to stop, another to click
        onView(withText("24: Ride Cymbal 1")).perform(click())
        onView(withText("24: Ride Cymbal 1")).perform(click())

        get("com.qfs.pagan.LineLabelStd", 3).perform(click())
        get(R.id.btnChoosePercussion).perform(click())
        onView(withText("11: Snare Drum 1")).perform(click())

        get("com.qfs.pagan.LineLabelStd", 4).perform(click())
        get(R.id.btnChoosePercussion).perform(click())
        onView(withText("8: Bass Drum 2")).perform(click())

        get("com.qfs.pagan.ColumnLabelView", 3).perform(click())
        get(R.id.btnInsertBeat).perform(longClick())
        get(R.id.etNumber).perform(click())
        get(R.id.etNumber).perform(typeTextIntoFocusedView("12"))
        onView(withText(android.R.string.ok)).perform(click())


        Thread.sleep(2000)

        //get(R.id.btnSplit).perform(click())
        //get("com.qfs.pagan.LeafButtonStd", 0).perform(longClick())
        //get("com.qfs.pagan.LineLabelStd", 0).perform(click())
        //get("com.qfs.pagan.LineLabelStd", 0).perform(click())
        //Thread.sleep(4000)


        //val numberSelectorButton = onView(
        //    allOf(
        //        withText("3"),
        //        childAtPosition(
        //            childAtPosition(
        //                withId(R.id.nsOctave),
        //                0
        //            ),
        //            6
        //        ),
        //        isDisplayed()
        //    )
        //)
        //numberSelectorButton.perform(click())

        //val numberSelectorButton2 = onView(
        //    allOf(
        //        withText("5"),
        //        childAtPosition(
        //            childAtPosition(
        //                withId(R.id.nsOffset),
        //                0
        //            ),
        //            10
        //        ),
        //        isDisplayed()
        //    )
        //)
        //numberSelectorButton2.perform(click())
    }

    private fun childAtPosition(
        parentMatcher: Matcher<View>, position: Int
    ): Matcher<View> {

        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("Child at position $position in parent ")
                parentMatcher.describeTo(description)
            }

            public override fun matchesSafely(view: View): Boolean {
                val parent = view.parent
                return parent is ViewGroup && parentMatcher.matches(parent)
                        && view == parent.getChildAt(position)
            }
        }
    }
}
