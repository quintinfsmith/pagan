package com.qfs.pagan


import androidx.test.espresso.DataInteraction
import androidx.test.espresso.ViewInteraction
import androidx.test.filters.LargeTest
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent

import androidx.test.InstrumentationRegistry.getInstrumentation
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.ViewMatchers.*

import com.qfs.pagan.R

import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import org.hamcrest.core.IsInstanceOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.anything
import org.hamcrest.Matchers.`is`

@LargeTest
@RunWith(AndroidJUnit4::class)
class MainActivityTest2 {

    @Rule
    @JvmField
    var mActivityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun mainActivityTest2() {
    val buttonStd = onView(
allOf(withId(R.id.btnFrontNew), withText("New Project"),
childAtPosition(
allOf(withId(R.id.linearLayout),
childAtPosition(
withClassName(`is`("androidx.constraintlayout.widget.ConstraintLayout")),
1)),
0),
isDisplayed()))
    buttonStd.perform(click())
    
    val leafButtonStd = onView(
allOf(childAtPosition(
childAtPosition(
withClassName(`is`("com.qfs.pagan.ColumnLayout")),
0),
0),
isDisplayed()))
    leafButtonStd.perform(click())
    
    val buttonIcon = onView(
allOf(withId(R.id.btnSplit), withContentDescription("Split this beat or subdivision"),
childAtPosition(
allOf(withId(R.id.clTextViews),
childAtPosition(
withClassName(`is`("android.widget.LinearLayout")),
0)),
0),
isDisplayed()))
    buttonIcon.perform(click())
    
    val numberSelectorButton = onView(
allOf(withText("2"),
childAtPosition(
childAtPosition(
withId(R.id.nsOctave),
0),
4),
isDisplayed()))
    numberSelectorButton.perform(click())
    
    val leafButtonStd2 = onView(
allOf(childAtPosition(
childAtPosition(
withClassName(`is`("com.qfs.pagan.ColumnLayout")),
0),
1),
isDisplayed()))
    leafButtonStd2.perform(click())
    
    val numberSelectorButton2 = onView(
allOf(withText("3"),
childAtPosition(
childAtPosition(
withId(R.id.nsOctave),
0),
6),
isDisplayed()))
    numberSelectorButton2.perform(click())
    
    val leafButtonStd3 = onView(
allOf(childAtPosition(
childAtPosition(
withClassName(`is`("com.qfs.pagan.ColumnLayout")),
0),
0),
isDisplayed()))
    leafButtonStd3.perform(longClick())
    
    val lineLabelCtlGlobal = onView(
allOf(childAtPosition(
childAtPosition(
withClassName(`is`("android.widget.LinearLayout")),
2),
0),
isDisplayed()))
    lineLabelCtlGlobal.perform(click())
    
    val leafButtonCtlGlobal = onView(
allOf(childAtPosition(
childAtPosition(
withClassName(`is`("com.qfs.pagan.ColumnLayout")),
2),
0),
isDisplayed()))
    leafButtonCtlGlobal.perform(click())
    
    val lineLabelCtlGlobal2 = onView(
allOf(childAtPosition(
childAtPosition(
withClassName(`is`("android.widget.LinearLayout")),
2),
0),
isDisplayed()))
    lineLabelCtlGlobal2.perform(click())
    
    val buttonStd2 = onView(
allOf(withText("120 BPM"),
childAtPosition(
childAtPosition(
withClassName(`is`("android.widget.LinearLayout")),
0),
0),
isDisplayed()))
    buttonStd2.perform(click())
    
    val rangedFloatInput = onView(
allOf(withId(R.id.etNumber), withText("120.0"),
childAtPosition(
childAtPosition(
withClassName(`is`("android.widget.LinearLayout")),
0),
0),
isDisplayed()))
    rangedFloatInput.perform(replaceText("140"))
    
    val rangedFloatInput2 = onView(
allOf(withId(R.id.etNumber), withText("140"),
childAtPosition(
childAtPosition(
withClassName(`is`("android.widget.LinearLayout")),
0),
0),
isDisplayed()))
    rangedFloatInput2.perform(closeSoftKeyboard())
    
    val materialButton = onView(
allOf(withId(android.R.id.button1), withText("OK"),
childAtPosition(
childAtPosition(
withClassName(`is`("android.widget.ScrollView")),
0),
3)))
    materialButton.perform(scrollTo(), click())
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
                return parent is ViewGroup && parentMatcher.matches(parent)
                        && view == parent.getChildAt(position)
            }
        }
    }
}
