package br.com.fenix.bilingualreader.view.ui.reader.book

import android.content.Intent
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.enums.Languages
import br.com.fenix.bilingualreader.model.enums.Libraries
import br.com.fenix.bilingualreader.service.parses.book.DocumentParse
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.utils.BookTestUtil
import junit.framework.TestCase.assertTrue
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4::class)
class BookReaderActivityTest {

    private val book: Book = BookTestUtil.getBook(ApplicationProvider.getApplicationContext(), BookTestUtil.BOOK_PATH)
    private var intent: Intent? = null

    init {
        DocumentParse.init(ApplicationProvider.getApplicationContext())

        //Clear cache
        BookTestUtil.clearCache(ApplicationProvider.getApplicationContext())

        assertTrue(
            "Book file informed not found, please verify declared 'mBookLocation' in " + BookTestUtil.BookTestUtils::class.java.name,
            book.file.exists()
        )

        book.bookMark = BookTestUtil.BOOK_PAGE
        book.language = Languages.JAPANESE

        intent = Intent(ApplicationProvider.getApplicationContext(), BookReaderActivity::class.java)

        val bundle = Bundle()
        bundle.putSerializable(GeneralConsts.KEYS.OBJECT.LIBRARY, book.library)
        bundle.putSerializable(GeneralConsts.KEYS.OBJECT.BOOK, book)
        bundle.putString(GeneralConsts.KEYS.BOOK.NAME, book.title)
        bundle.putInt(GeneralConsts.KEYS.BOOK.PAGE_NUMBER, BookTestUtil.BOOK_PAGE)
        bundle.putInt(GeneralConsts.KEYS.BOOK.MARK, 0)
        intent?.putExtras(bundle)
    }


    @get:Rule
    val activityScenarioRule = ActivityScenarioRule<BookReaderActivity>(intent)

    private val awaitProcessSeconds = 2L

    @Test
    fun `0_test_book_reader`() {
        val waiter = CountDownLatch(1)
        val scenario = activityScenarioRule.scenario

        scenario.onActivity {
            val fragment = it.supportFragmentManager.findFragmentById(R.id.root_frame_book_reader)
            assertTrue(fragment is BookReaderFragment)
        }

        waiter.await(10, TimeUnit.MINUTES)
    }

    @Test
    fun `1_test_book_reader`() {
        val waiter = CountDownLatch(1)
        val scenario = activityScenarioRule.scenario

        scenario.onActivity {
            val fragment = it.supportFragmentManager.findFragmentById(R.id.root_frame_book_reader)
            assertTrue(fragment is BookReaderFragment)
        }

        waiter.await(awaitProcessSeconds, TimeUnit.SECONDS)
        //onView(withId(R.id.root_frame_book_reader)).perform(click())


        waiter.await(10, TimeUnit.MINUTES)
    }

}