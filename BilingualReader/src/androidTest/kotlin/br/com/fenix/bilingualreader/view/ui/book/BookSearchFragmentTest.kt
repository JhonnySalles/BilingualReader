package br.com.fenix.bilingualreader.view.ui.book

import android.content.Intent
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.service.parses.book.DocumentParse
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.utils.BookTestUtil
import br.com.fenix.bilingualreader.view.ui.menu.MenuActivity
import junit.framework.TestCase
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4::class)
class BookSearchFragmentTest {

    private val book: Book = BookTestUtil.getBook(ApplicationProvider.getApplicationContext(), "/storage/1D01-1E06/Livros/" + "Russian-Roulette-epub.epub")
    private var intent: Intent? = null

    init {
        DocumentParse.init(ApplicationProvider.getApplicationContext())

        //Clear cache
        BookTestUtil.clearCache(ApplicationProvider.getApplicationContext())

        TestCase.assertTrue(
            "Book informed not found, please verify book in " + BookSearchFragmentTest::class.java.name,
            book.file.exists()
        )
    }


    private val awaitProcess = 2L
    private val fontSize = GeneralConsts.KEYS.READER.BOOK_PAGE_FONT_SIZE_DEFAULT

    init {
        intent = Intent(ApplicationProvider.getApplicationContext(), MenuActivity::class.java)
        val bundle = Bundle()

        bundle.putInt(GeneralConsts.KEYS.FRAGMENT.ID, R.id.frame_book_search)
        bundle.putSerializable(GeneralConsts.KEYS.OBJECT.BOOK, book)
        bundle.putString(GeneralConsts.KEYS.OBJECT.DOCUMENT_PATH, book.path)
        bundle.putString(GeneralConsts.KEYS.OBJECT.DOCUMENT_PASSWORD, book.password)
        bundle.putInt(GeneralConsts.KEYS.OBJECT.DOCUMENT_FONT_SIZE, fontSize.toInt())
        bundle.putBoolean(GeneralConsts.KEYS.OBJECT.DOCUMENT_JAPANESE_STYLE, false)

        intent?.putExtras(bundle)
    }


    @get:Rule
    val activityScenarioRule = ActivityScenarioRule<MenuActivity>(intent)

    @Test
    fun `1_test_book_search`() {

        val waiter = CountDownLatch(1)
        waiter.await(10, TimeUnit.MINUTES)
    }

}