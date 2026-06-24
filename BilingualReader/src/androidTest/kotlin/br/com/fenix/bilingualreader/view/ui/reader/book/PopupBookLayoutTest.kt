package br.com.fenix.bilingualreader.view.ui.reader.book

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.TestActivity
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PopupBookLayoutTest {

    private fun launchFragment() {
        val scenario = ActivityScenario.launch(TestActivity::class.java)
        scenario.onActivity { activity ->
            val fragment = PopupBookLayout()
            activity.setFragment(fragment)
        }
    }

    @Test
    fun testViewsAreDisplayed() {
        launchFragment()
        onView(withId(R.id.popup_book_layout_container)).check(matches(isDisplayed()))
        onView(withId(R.id.popup_book_layout_scrolling_mode)).check(matches(isDisplayed()))
        onView(withId(R.id.popup_book_layout_alignment_justify)).check(matches(isDisplayed()))
    }

    @Test
    fun testAlignmentButtonsClick() {
        launchFragment()
        onView(withId(R.id.popup_book_layout_alignment_left)).perform(click())
        onView(withId(R.id.popup_book_layout_alignment_center)).perform(click())
        onView(withId(R.id.popup_book_layout_alignment_right)).perform(click())
    }
}
