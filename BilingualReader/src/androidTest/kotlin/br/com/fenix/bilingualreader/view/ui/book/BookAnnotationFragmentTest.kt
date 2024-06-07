package br.com.fenix.bilingualreader.view.ui.book

import android.content.Intent
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.BookAnnotation
import br.com.fenix.bilingualreader.model.enums.Color
import br.com.fenix.bilingualreader.model.enums.MarkType
import br.com.fenix.bilingualreader.service.parses.book.DocumentParse
import br.com.fenix.bilingualreader.service.repository.BookAnnotationRepository
import br.com.fenix.bilingualreader.service.repository.BookRepository
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.FontUtil
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
class BookAnnotationFragmentTest {

    private val book: Book = BookTestUtil.getBook(ApplicationProvider.getApplicationContext(), "/storage/1CF6-3F0F/Livro/" + "[Novel] Mahoka Koko no Rettosei Shiba Tatsuya Ansatsu Keikaku 01.epub")
    private var intent: Intent? = null

    init {
        DocumentParse.init(ApplicationProvider.getApplicationContext())

        //Clear cache
        BookTestUtil.clearCache(ApplicationProvider.getApplicationContext())

        TestCase.assertTrue(
            "Book informed not found, please verify book in " + BookAnnotationFragmentTest::class.java.name,
            book.file.exists()
        )
    }


    private val awaitProcess = 2L
    private val fontSize = GeneralConsts.KEYS.READER.BOOK_PAGE_FONT_SIZE_DEFAULT

    init {
        try {
            val repository = BookAnnotationRepository(ApplicationProvider.getApplicationContext())

            val annotations = mutableListOf<BookAnnotation>()
            annotations.add(BookAnnotation(book.id!!, 1, book.pages, MarkType.Annotation, 1f, "Chapter 1", "Text annotation", intArrayOf(1,5), "Annotation 1", true, Color.Yellow))
            annotations.add(BookAnnotation(book.id!!, 2, book.pages, MarkType.Annotation, 2f, "Chapter 2", "Text annotation chapter 2", intArrayOf(2,6), "Annotation 2", true, Color.Red))
            annotations.add(BookAnnotation(book.id!!, 2, book.pages, MarkType.Annotation, 2f, "Chapter 2", "Text annotation chapter 2", intArrayOf(9,15), "Annotation 3", true, Color.Green))
            annotations.add(BookAnnotation(book.id!!, 3, book.pages, MarkType.PageMark, 3f, "Chapter 3", "Text page mark", intArrayOf(9,15), "Page mark 1", true, Color.None))
            annotations.add(BookAnnotation(book.id!!, 4, book.pages, MarkType.PageMark, 5f, "Chapter 5", "Text page mark", intArrayOf(), "Page mark 2", true, Color.None))
            annotations.add(BookAnnotation(book.id!!, 4, book.pages, MarkType.Annotation, 5f, "Chapter 5", "Text annotation chapter 5", intArrayOf(9,15), "Annotation 4", true, Color.Blue))

            for (annotation in annotations)
                repository.save(annotation)
        } catch (e : Exception) {
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
        waiter.await(10, TimeUnit.MINUTES)
    }

}