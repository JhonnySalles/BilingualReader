package br.com.fenix.bilingualreader.view.ui.popup

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.TestActivity
import br.com.fenix.bilingualreader.model.entity.Vocabulary
import br.com.fenix.bilingualreader.service.japanese.Formatter
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PopupVocabularyTest {

    @Before
    fun setup() {
        mockkObject(Formatter)
    }

    @After
    fun teardown() {
        unmockkObject(Formatter)
    }

    private fun createMockVocabulary(): Vocabulary {
        return Vocabulary(
            id = 1L,
            word = listOf("日本語"),
            reading = "にほんご",
            portuguese = "Língua Japonesa",
            english = "Japanese Language",
            jlpt = 5,
            appears = 10,
            basicForm = "日本語"
        )
    }

    @Test
    fun testPopupVocabularyDisplay() {
        val vocab = createMockVocabulary()
        every { Formatter.getVocabulary(1L) } returns vocab
        every { Formatter.getKanjax(any()) } returns null

        val scenario = ActivityScenario.launch(TestActivity::class.java)
        scenario.onActivity { activity ->
            val popup = PopupVocabulary(activity, false)
            popup.getPopupVocabulary(1L)
        }

        onView(withId(R.id.popup_vocabulary_title)).check(matches(withText("日本語")))
        onView(withId(R.id.popup_vocabulary_meaning_portuguese)).check(matches(withText("Língua Japonesa")))
        onView(withId(R.id.popup_vocabulary_meaning_english)).check(matches(withText("Japanese Language")))
        onView(withId(R.id.popup_vocabulary_jlpt)).check(matches(withText(containsString("5"))))
    }
}
