package br.com.fenix.bilingualreader.view.ui.book

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.TestActivity
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.enums.Libraries
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.anyOf
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class BookSearchFragmentTest {

    private fun createMockBook(context: Context): Book {
        val tempFile = File(context.cacheDir, "test_path.epub")
        if (!tempFile.exists()) tempFile.createNewFile()
        return Book(null, 1L, tempFile)
    }

    private fun launchFragment() {
        val scenario = ActivityScenario.launch(TestActivity::class.java)
        scenario.onActivity { activity ->
            val book = createMockBook(activity)
            val fragment = BookSearchFragment()
            val args = android.os.Bundle()
            args.putSerializable(GeneralConsts.KEYS.OBJECT.BOOK, book)
            args.putString(GeneralConsts.KEYS.OBJECT.DOCUMENT_PATH, book.path)
            args.putString(GeneralConsts.KEYS.OBJECT.DOCUMENT_PASSWORD, "")
            args.putInt(GeneralConsts.KEYS.OBJECT.DOCUMENT_FONT_SIZE, 12)
            args.putBoolean(GeneralConsts.KEYS.OBJECT.DOCUMENT_JAPANESE_STYLE, false)
            fragment.arguments = args
            
            activity.setFragment(fragment)
        }
    }

    @Test
    fun testViewsAreDisplayed() {
        launchFragment()
        onView(withId(R.id.toolbar_book_search)).check(matches(isDisplayed()))
        onView(withId(R.id.book_search_history_content)).check(matches(anyOf(isDisplayed(), withEffectiveVisibility(Visibility.GONE))))
        onView(withId(R.id.book_search_recycler_view)).check(matches(anyOf(isDisplayed(), withEffectiveVisibility(Visibility.GONE))))
    }

    @Test
    fun testHistoryClearButtonClick() {
        launchFragment()
        // O histórico pode estar oculto inicialmente, mas o botão deve estar lá no layout
        onView(withId(R.id.book_search_history_clear)).perform(scrollTo(), click())
    }
}
