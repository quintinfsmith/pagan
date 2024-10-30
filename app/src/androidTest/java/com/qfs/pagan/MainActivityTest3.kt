package com.qfs.pagan


import android.app.Instrumentation
import android.view.View
import android.view.ViewGroup
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.action.ViewActions.swipeRight
import androidx.test.espresso.action.ViewActions.swipeUp
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
        val inst = Instrumentation()
        get(R.id.btnFrontNew).perform(click())

        // Jump to last beat
        inst.sendStringSync("B")

        // Insert 12 beats
        inst.sendStringSync("12i")

        // Jump to Percussion (last visisble Channel)
        inst.sendStringSync("C")

        // Insert 3 new lines
        inst.sendStringSync("3i")

        get(R.id.btnChoosePercussion).perform(click())
        get(R.id.rvOptions).perform( swipeUp() )

        // Tap twice, once to stop, another to click
        onView(withChild(withText("22: Crash Cymbal 1"))).perform(click())
        onView(withChild(withText("22: Crash Cymbal 1"))).perform(click())

        // Move to next line down
        inst.sendStringSync("j")

        get(R.id.btnChoosePercussion).perform(click())
        get(R.id.rvOptions).perform( swipeUp() )

        // Tap twice, once to stop, another to click
        onView(withText("24: Ride Cymbal 1")).perform(click())
        onView(withText("24: Ride Cymbal 1")).perform(click())


        // Move to next line down
        inst.sendStringSync("j")

        get(R.id.btnChoosePercussion).perform(click())
        onView(withText("11: Snare Drum 1")).perform(click())

        // Move to next line down
        inst.sendStringSync("j")

        get(R.id.btnChoosePercussion).perform(click())
        onView(withText("8: Bass Drum 2")).perform(click())

        get("com.qfs.pagan.CompoundScrollView").perform(swipeRight())

        // Jump to first beat
        inst.sendStringSync("0B")
        // Move to first cell in 2nd channel down
        inst.sendStringSync("2J")
        // Move to the 2nd line in the current channel
        inst.sendStringSync("j")
        // Split current leaf
        inst.sendStringSync("s")
        // Set the percussion event
        get(R.id.btnUnset).perform(click())
        // Move to next leaf
        inst.sendStringSync("l")
        // Set the percussion event
        get(R.id.btnUnset).perform(click())
        // Move to the first leaf in the 2nd next line down
        inst.sendStringSync("2j")

        // Insert leaf
        inst.sendStringSync("i")
        // Set the percussion event
        get(R.id.btnUnset).perform(click())
        // Move to next leaf
        inst.sendStringSync("l")
        // Set the percussion event
        get(R.id.btnUnset).perform(click())

       // // dup ride cymbals across line
       get("com.qfs.pagan.LeafButtonStd", 2).perform(longClick())
       get("com.qfs.pagan.LineLabelStd", 2).perform(click())

        inst.sendStringSync("1B2J2j")
        inst.sendStringSync("4s")
        inst.sendStringSync("[")
        inst.sendStringSync("3l")
        inst.sendStringSync("[")
        inst.sendStringSync("l")
        inst.sendStringSync("3a")
        inst.sendStringSync("l")
        inst.sendStringSync("[")
        inst.sendStringSync("j")
        inst.sendStringSync("s")
        inst.sendStringSync("l")
        inst.sendStringSync("s")
        inst.sendStringSync("[")
        inst.sendStringSync("l")
        inst.sendStringSync("[")
        inst.sendStringSync("kL")
        inst.sendStringSync("4s")
        inst.sendStringSync("[")
        inst.sendStringSync("3l")
        inst.sendStringSync("[")
        inst.sendStringSync("lj")
        inst.sendStringSync("s")
        inst.sendStringSync("[")
        inst.sendStringSync("l")
        inst.sendStringSync("[")
        inst.sendStringSync("5B2J2j")
        inst.sendStringSync("4s")
        inst.sendStringSync("[")
        inst.sendStringSync("3l")
        inst.sendStringSync("[")
        inst.sendStringSync("l")
        inst.sendStringSync("4s")
        inst.sendStringSync("l")
        inst.sendStringSync("[")
        inst.sendStringSync("j")
        inst.sendStringSync("4s")
        inst.sendStringSync("2l")
        inst.sendStringSync("[")
        inst.sendStringSync("l")
        inst.sendStringSync("[")
        // Beat 7
        inst.sendStringSync("lk")
        inst.sendStringSync("4s[3l[")
        // Beat 8
        inst.sendStringSync("lj")
        inst.sendStringSync("s")
        inst.sendStringSync("[l[")
        // beat 9
        inst.sendStringSync("lk")
        inst.sendStringSync("4s[3l[")
        // Beat 10
        inst.sendStringSync("l")
        inst.sendStringSync("ss")
        inst.sendStringSync("l")
        inst.sendStringSync("[")
        inst.sendStringSync("j")
        inst.sendStringSync("s")
        inst.sendStringSync("l")
        inst.sendStringSync("[")
        // Beat 11
        inst.sendStringSync("lk")
        inst.sendStringSync("s")
        inst.sendStringSync("l")
        inst.sendStringSync("[")
        // Beat 12
        inst.sendStringSync("l")
        inst.sendStringSync("ss")
        inst.sendStringSync("l")
        inst.sendStringSync("[")
        inst.sendStringSync("j")
        inst.sendStringSync("sls")
        inst.sendStringSync("[")
        inst.sendStringSync("l")
        inst.sendStringSync("[")
        // Beat 13
        inst.sendStringSync("lk")
        inst.sendStringSync("4s")
        inst.sendStringSync("[")
        inst.sendStringSync("3l")
        inst.sendStringSync("[")
        // Beat 14
        inst.sendStringSync("l")
        inst.sendStringSync("ssl[")
        inst.sendStringSync("jsl[")
        inst.sendStringSync("3k")
        inst.sendStringSync("sl[")
        inst.sendStringSync("jlx")
        // Beat 15
        inst.sendStringSync("ls")
        inst.sendStringSync("[")
        inst.sendStringSync("l")
        inst.sendStringSync("[")
        inst.sendStringSync("jsl")
        inst.sendStringSync("[")














       // get("com.qfs.pagan.LeafButtonStd", 11).perform(click())
       // get(R.id.btnInsert).perform(click())
       // get(R.id.btnInsert).perform(click())
       // get(R.id.btnInsert).perform(click())
       // get("com.qfs.pagan.LeafButtonStd", 11).perform(click())
       // get(R.id.btnUnset).perform(click())
       // get("com.qfs.pagan.LeafButtonStd", 14).perform(click())
       // get(R.id.btnUnset).perform(click())


       // get("com.qfs.pagan.LeafButtonStd", 20).perform(click())
       // get(R.id.btnSplit).perform(longClick())
       // get(R.id.etNumber).perform(click())
       // get(R.id.etNumber).perform(typeTextIntoFocusedView("4"))
       // onView(withText(android.R.string.ok)).perform(click())
       // get("com.qfs.pagan.LeafButtonStd", 21).perform(click())
       // get(R.id.btnUnset).perform(click())

        Thread.sleep(5000)
       // onView(
       //     allOf(
       //         withText("2"),
       //         withClassName(`is`("com.qfs.pagan.ColumnLabelView")),
       //         isDisplayed()
       //     )
       // ).perform(click())

       // get("com.qfs.pagan.LeafButtonStd", 24).perform(click())
       // get(R.id.btnSplit).perform(longClick())
       // get(R.id.etNumber).perform(click())
       // onView(withText(android.R.string.ok)).perform(click())

       // // Next button out of view, scroll
       // get("com.qfs.pagan.CompoundScrollView").perform(slowSwipeLeft())
       // onView(
       //     allOf(
       //         withText("3"),
       //         withClassName(`is`("com.qfs.pagan.ColumnLabelView")),
       //         isDisplayed()
       //     )
       // ).perform(click())

       // //allOf(
       // //    withText("3"),
       // //    withClassName(`is`("com.qfs.pagan.ColumnLabelView")),
       // //    isDisplayed()
       // //)


       // get("com.qfs.pagan.LeafButtonStd", 16).perform(click())
       // get(R.id.btnSplit).perform(longClick())

       // // Assume 4 is now the default value and press ok
       // onView(withText(android.R.string.ok)).perform(click())
       // get("com.qfs.pagan.LeafButtonStd", 17).perform(click())
       // get(R.id.btnUnset).perform(click())
       // get("com.qfs.pagan.LeafButtonStd", 18).perform(click())
       // get(R.id.btnUnset).perform(click())


       // Thread.sleep(2000)

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
