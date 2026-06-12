package br.com.fenix.bilingualreader.view.ui.reader.manga

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
class PopupMangaColorFilterFragmentTest {

    private fun launchFragment() {
        val scenario = ActivityScenario.launch(TestActivity::class.java)
        scenario.onActivity { activity ->
            val fragment = PopupMangaColorFilterFragment()
            activity.setFragment(fragment)
        }
    }

    @Test
    fun testViewsAreDisplayed() {
        launchFragment()
        onView(withId(R.id.popup_manga_switch_color_filter)).check(matches(isDisplayed()))
        onView(withId(R.id.popup_manga_seekbar_color_filter_red)).check(matches(isDisplayed()))
        onView(withId(R.id.popup_manga_switch_blue_light)).check(matches(isDisplayed()))
    }

    @Test
    fun testTogglesClick() {
        launchFragment()
        onView(withId(R.id.popup_manga_switch_color_filter)).perform(click())
        onView(withId(R.id.popup_manga_switch_grayscale)).perform(click())
        onView(withId(R.id.popup_manga_switch_invert_color)).perform(click())
    }
}
