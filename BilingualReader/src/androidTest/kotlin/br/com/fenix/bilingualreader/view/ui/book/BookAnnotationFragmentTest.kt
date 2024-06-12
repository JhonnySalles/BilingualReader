package br.com.fenix.bilingualreader.view.ui.book

import android.content.Intent
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.BookAnnotation
import br.com.fenix.bilingualreader.service.parses.book.DocumentParse
import br.com.fenix.bilingualreader.service.repository.DataBase
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.utils.BookTestUtil
import br.com.fenix.bilingualreader.view.ui.menu.MenuActivity
import junit.framework.TestCase
import org.junit.AfterClass
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4::class)
class BookAnnotationFragmentTest {

    private val book: Book = BookTestUtil.getBook(ApplicationProvider.getApplicationContext(), BookTestUtil.BOOK_PATH)
    private var intent: Intent? = null

    init {
        DocumentParse.init(ApplicationProvider.getApplicationContext())

        //Clear cache
        BookTestUtil.clearCache(ApplicationProvider.getApplicationContext())

        TestCase.assertTrue(
            "Book file informed not found, please verify declared 'mBookLocation' in " + BookTestUtil.BookTestUtils::class.java.name,
            book.file.exists()
        )
    }


    private val awaitProcess = 2L
    private val fontSize = GeneralConsts.KEYS.READER.BOOK_PAGE_FONT_SIZE_DEFAULT

    companion object {
        private val annotations = mutableListOf<BookAnnotation>()

        @JvmStatic
        @AfterClass
        fun clear(): Unit {
            val repository = DataBase.getDataBase(ApplicationProvider.getApplicationContext()).getBookAnnotation()
            for (annotation in annotations)
                repository.delete(annotation)
        }
    }

    init {
        val repository = DataBase.getDataBase(ApplicationProvider.getApplicationContext()).getBookAnnotation()
        annotations.clear()
        annotations.addAll(BookTestUtil.getAnnotations(book))
        for (annotation in annotations) {
            repository.find(annotation.id!!)?.let { repository.delete(it) }
            repository.save(annotation)
        }
    }

    init {
        intent = Intent(ApplicationProvider.getApplicationContext(), MenuActivity::class.java)
        val bundle = Bundle()

        bundle.putInt(GeneralConsts.KEYS.FRAGMENT.ID, R.id.frame_book_annotation)
        bundle.putSerializable(GeneralConsts.KEYS.OBJECT.BOOK, book)
        bundle.putString(GeneralConsts.KEYS.OBJECT.DOCUMENT_PATH, book.path)
        bundle.putString(GeneralConsts.KEYS.OBJECT.DOCUMENT_PASSWORD, book.password)
        bundle.putInt(GeneralConsts.KEYS.OBJECT.DOCUMENT_FONT_SIZE, fontSize.toInt())

        intent?.putExtras(bundle)
    }


    @get:Rule
    val activityScenarioRule = ActivityScenarioRule<MenuActivity>(intent)

    @Test
    fun `1_test_book_annotation`() {
        val waiter = CountDownLatch(1)
        val scenario = activityScenarioRule.scenario

        scenario.onActivity {
            val fragment = it.supportFragmentManager.findFragmentById(R.id.frame_book_annotation)
            TestCase.assertTrue(fragment is BookAnnotationFragment)
        }

        waiter.await(10, TimeUnit.MINUTES)
    }

}