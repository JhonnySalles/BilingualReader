package br.com.fenix.bilingualreader.view.ui.detail.book

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
import br.com.fenix.bilingualreader.model.enums.Libraries
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.repository.DataBase
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.view.ui.detail.DetailActivity
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class BookDetailFragmentTest {

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

        // Setup Library
        val mockPath = File(context.cacheDir, "mock_books_detail")
        if (!mockPath.exists()) mockPath.mkdirs()
        
        mockLib = Library(
            id = 1L,
            title = "Test Library",
            path = mockPath.absolutePath,
            language = Libraries.ENGLISH,
            type = Type.BOOK
        )
        db.getLibrariesDao().save(mockLib)

        // Setup Book
        val bookFile = File(mockPath, "book_test.epub")
        if (!bookFile.exists()) bookFile.createNewFile()

        mockBook = Book(100L, mockLib.id!!, "Detail Test Book", bookFile).apply {
            author = "Test Author"
            pages = 100
            bookMark = 50
            favorite = false
        }
        db.getBookDao().save(mockBook)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun getStartIntent(): Intent {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return Intent(context, DetailActivity::class.java).apply {
            putExtra(GeneralConsts.KEYS.OBJECT.LIBRARY, mockLib)
            putExtra(GeneralConsts.KEYS.OBJECT.BOOK, mockBook)
        }
    }

    @Test
    fun testBookDetailDisplay() {
        ActivityScenario.launch<DetailActivity>(getStartIntent())
        
        // Aguarda carregamento do ViewModel e renderização
        Thread.sleep(2000)

        // Verifica os campos principais
        onView(withId(R.id.book_detail_title)).check(matches(withText(mockBook.title)))
        onView(withId(R.id.book_detail_author)).check(matches(withText(mockBook.author)))
        onView(withId(R.id.book_detail_folder)).check(matches(withText(mockBook.path)))
        
        // Verifica o progresso (texto)
        // O formato no código é: "${it.bookMark} / ${it.pages} (${Util.formatDecimal(percent)} %)"
        // Para 50/100 é "50 / 100 (50,00 %)" ou similar dependendo do locale
        onView(withId(R.id.book_detail_book_mark)).check(matches(withText(org.hamcrest.Matchers.containsString("50 / 100"))))
    }

    @Test
    fun testFavoriteToggle() {
        ActivityScenario.launch<DetailActivity>(getStartIntent())
        
        Thread.sleep(2000)

        // Inicialmente não é favorito no DB
        assertFalse(db.getBookDao().get(mockBook.id!!)!!.favorite)

        // Clica no botão de favorito
        onView(withId(R.id.book_detail_button_favorite)).perform(click())
        
        // Aguarda persistência assíncrona do ViewModel
        Thread.sleep(500)
        
        // Verifica se o valor mudou no Banco de Dados
        assertTrue("O estado de favorito não foi persistido no Banco de Dados", 
            db.getBookDao().get(mockBook.id!!)!!.favorite)
            
        // Clica novamente para desmarcar
        onView(withId(R.id.book_detail_button_favorite)).perform(click())
        Thread.sleep(500)
        assertFalse(db.getBookDao().get(mockBook.id!!)!!.favorite)
    }

    @Test
    fun testDeleteDialogAppearance() {
        ActivityScenario.launch<DetailActivity>(getStartIntent())
        
        Thread.sleep(2000)

        // Clica no botão de deletar
        onView(withId(R.id.book_detail_button_delete)).perform(scrollTo(), click())
        
        // Verifica se o diálogo de confirmação apareceu
        onView(withText(R.string.book_library_menu_delete)).check(matches(isDisplayed()))
        
        // Cancela a ação
        onView(withText(R.string.action_negative)).perform(click())
        
        // Verifica se o diálogo sumiu
        Thread.sleep(500)
        onView(withText(R.string.book_library_menu_delete)).check(doesNotExist())
    }

    @Test
    fun testMarkReadButton() {
        ActivityScenario.launch<DetailActivity>(getStartIntent())
        
        Thread.sleep(2000)

        // Clica no botão de marcar como lido
        onView(withId(R.id.book_detail_button_mark_read)).perform(scrollTo(), click())
        
        Thread.sleep(500)
        
        // No repositório, markRead define bookMark = pages e seta data de finalização
        val updatedBook = db.getBookDao().get(mockBook.id!!)!!
        assertEquals(updatedBook.pages, updatedBook.bookMark)
    }
    
    private fun assertEquals(expected: Any?, actual: Any?) {
        if (expected != actual) {
            throw AssertionError("Expected: $expected but was: $actual")
        }
    }
}
