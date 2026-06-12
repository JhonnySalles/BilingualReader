package br.com.fenix.bilingualreader.view.ui.book

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.TestActivity
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookAnnotationFragmentTest {

    private fun createMockBook(context: android.content.Context): Book {
        val tempFile = java.io.File(context.cacheDir, "test_path.epub")
        if (!tempFile.exists()) tempFile.createNewFile()
        return Book(null, 1L, tempFile)
    }

    private fun launchFragment() {
        val scenario = ActivityScenario.launch(TestActivity::class.java)
        scenario.onActivity { activity ->
            val book = createMockBook(activity)
            val fragment = BookAnnotationFragment()
            val args = android.os.Bundle()
            args.putSerializable(GeneralConsts.KEYS.OBJECT.BOOK, book)
            args.putInt(GeneralConsts.KEYS.MANGA.PAGE_NUMBER, 0)
            fragment.arguments = args
            activity.setFragment(fragment)
        }
    }

    @Test
    fun testViewsAreDisplayed() {
        launchFragment()
        onView(withId(R.id.book_annotation_root)).check(matches(isDisplayed()))
        onView(withId(R.id.book_annotation_recycler_view)).check(matches(isDisplayed()))
        onView(withId(R.id.toolbar_book_annotation)).check(matches(isDisplayed()))
    }
}
