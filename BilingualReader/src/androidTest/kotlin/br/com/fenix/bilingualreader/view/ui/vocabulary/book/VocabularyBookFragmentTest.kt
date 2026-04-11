package br.com.fenix.bilingualreader.view.ui.vocabulary.book

import android.content.Context
import android.content.Intent
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.entity.Vocabulary
import br.com.fenix.bilingualreader.model.enums.Libraries
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.repository.DataBase
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.view.ui.vocabulary.VocabularyActivity
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class VocabularyBookFragmentTest {

    private lateinit var db: DataBase
    private lateinit var mockBook: Book
    private lateinit var mockLib: Library

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        // Setup Database
        db = Room.inMemoryDatabaseBuilder(context, DataBase::class.java)
            .allowMainThreadQueries()
            .build()
        DataBase.setTestingInstance(db)

        // Setup Library & Book file para evitar NPE nas capas
        val mockPath = File(context.cacheDir, "mock_book_vocab")
        if (!mockPath.exists()) mockPath.mkdirs()
        mockLib = Library(2L, "Vocab Book Lib", mockPath.absolutePath, Libraries.ENGLISH, Type.BOOK)
        db.getLibrariesDao().save(mockLib)

        val bookFile = File(mockPath, "vocab_book.epub")
        if (!bookFile.exists()) bookFile.createNewFile()
        mockBook = Book(mockLib.id, 600L, bookFile)
        db.getBookDao().save(mockBook)

        // Injeta dados de vocabulário associados ao livro
        val mockData = listOf(
            Vocabulary(id = 20L, word = "BookWord", reading = "ReadingB", english = "MeaningB", portuguese = null, basicForm = null, jlpt = 0, revised = false, favorite = false, appears = 0)
        )
        mockData.forEach { db.getVocabularyDao().save(it) }
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun getStartIntent(): Intent {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return Intent(context, VocabularyActivity::class.java).apply {
            putExtra(GeneralConsts.KEYS.VOCABULARY.TYPE, Type.BOOK)
            putExtra(GeneralConsts.KEYS.OBJECT.BOOK, mockBook)
            action = Intent.ACTION_MAIN
        }
    }

    @Test
    fun testVocabularyBookRendering() {
        ActivityScenario.launch<VocabularyActivity>(getStartIntent())
        
        Thread.sleep(3000)

        // Verifica se o título do livro foi injetado no EditText
        onView(withId(R.id.vocabulary_book_edittext)).check(matches(withText(mockBook.name)))
        
        // Verifica se a palavra associada a este livro aparece na lista
        onView(withText("BookWord")).check(matches(isDisplayed()))
    }

    @Test
    fun testOrderBottomSheetTrigger() {
        ActivityScenario.launch<VocabularyActivity>(getStartIntent())
        
        Thread.sleep(2000)

        // Clique longo no menu de ordem
        onView(withId(R.id.menu_vocabulary_list_order)).perform(longClick())
        
        Thread.sleep(1000)
        
        // Verifica se o BottomSheet respectivo ao Book apareceu
        onView(withId(R.id.vocabulary_book_popup_menu_order_filter)).check(matches(isDisplayed()))
    }
}
