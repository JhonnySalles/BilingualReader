package br.com.fenix.bilingualreader.view.ui.popup

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.TestActivity
import br.com.fenix.bilingualreader.model.enums.TextSpeech
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PopupTTSTest {

    @Test
    fun testPopupTTSDisplay() {
        val scenario = ActivityScenario.launch(TestActivity::class.java)
        scenario.onActivity { activity ->
            val popup = PopupTTS(activity)
            popup.getPopupTTS(TextSpeech.getDefault(false), 1.0f) { _, _ -> }
        }

        onView(withText(R.string.action_confirm)).check(matches(isDisplayed()))
        onView(withId(R.id.popup_tts_voice)).check(matches(isDisplayed()))
        onView(withId(R.id.popup_tts_speed)).check(matches(isDisplayed()))
    }

    @Test
    fun testCancelDismissesPopup() {
        val scenario = ActivityScenario.launch(TestActivity::class.java)
        scenario.onActivity { activity ->
            val popup = PopupTTS(activity)
            popup.getPopupTTS(TextSpeech.getDefault(false), 1.0f) { _, _ -> }
        }

        onView(withText(R.string.action_cancel)).perform(click())
        onView(withText(R.string.action_confirm)).check(androidx.test.espresso.assertion.ViewAssertions.doesNotExist())
    }
}
