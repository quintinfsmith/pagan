package com.qfs.pagan


import android.view.View
import android.view.ViewGroup
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withClassName
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.hamcrest.BaseMatcher
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
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

    private fun <T> nth(n: Int, matcher: Matcher<T>): Matcher<T> {
        return object : BaseMatcher<T>() {
            var match_count = 0

            override fun matches(item: Any): Boolean {
                if (matcher.matches(item)) {
                    return this.match_count++ == n
                }

                return false
            }

            override fun describeTo(description: Description) {
                description.appendText("should return first matching item")
            }
        }
    }
    @Test
    fun mainActivityTest3() {
        val buttonStd = onView(
            allOf(
                withId(R.id.btnFrontNew),
                isDisplayed()
            )
        )
        buttonStd.perform(click())

        val leafButtonStd = onView(
            allOf(
                nth(1, withClassName(`is`("com.qfs.pagan.LeafButtonStd"))),
                isDisplayed()
            )
        )
        leafButtonStd.perform(click())

        val buttonIcon = onView(
            allOf(
                withId(R.id.btnSplit),
                isDisplayed()
            )
        )
        buttonIcon.perform(click())

        // val leafButtonStd2 = onView(
        //     allOf(
        //         childAtPosition(
        //             childAtPosition(
        //                 withClassName(`is`("com.qfs.pagan.ColumnLayout")),
        //                 0
        //             ),
        //             1
        //         ),
        //         isDisplayed()
        //     )
        // )
        // leafButtonStd2.perform(click())

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
