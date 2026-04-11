package br.com.fenix.bilingualreader.view.ui.reader.book

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
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class BookReaderActivityTest {

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
        val mockPath = File(context.cacheDir, "mock_books_reader")
        if (!mockPath.exists()) mockPath.mkdirs()
        
        mockLib = Library(
            id = 1L,
            title = "Book Test Library",
            path = mockPath.absolutePath,
            language = Libraries.ENGLISH,
            type = Type.BOOK
        )
        db.getLibrariesDao().save(mockLib)

        // Setup Book
        val bookFile = File(mockPath, "book_test.epub")
        if (!bookFile.exists()) bookFile.createNewFile()

        mockBook = Book(mockLib.id, 500L, bookFile).apply {
            title = "Reader UI Test Book"
            author = "Test Author"
            pages = 300
            bookMark = 50
        }
        db.getBookDao().save(mockBook)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun getStartIntent(): Intent {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return Intent(context, BookReaderActivity::class.java).apply {
            putExtra(GeneralConsts.KEYS.OBJECT.LIBRARY, mockLib)
            putExtra(GeneralConsts.KEYS.OBJECT.BOOK, mockBook)
            // Simula clique em um livro
            action = Intent.ACTION_MAIN
        }
    }

    @Test
    fun testHUDMetadataDisplay() {
        ActivityScenario.launch<BookReaderActivity>(getStartIntent())
        
        // Aguarda transição e renderização do HUD
        Thread.sleep(2000)

        // Verifica o título na Toolbar
        // Note: Em alguns layouts o título pode estar no R.id.reader_book_toolbar_title
        try {
            onView(withId(R.id.reader_book_toolbar_title)).check(matches(withText(mockBook.name)))
        } catch (e: Exception) {
            // Fallback para o título nativo da toolbar se o custom view não estiver presente
            onView(withText(mockBook.name)).check(matches(isDisplayed()))
        }
        
        // Verifica o autor no rodapé
        onView(withId(R.id.reader_book_toolbar_bottom_author)).check(matches(withText(mockBook.author)))
        
        // Verifica se a barra de progresso do fundo existe
        onView(withId(R.id.reader_book_bottom_progress)).check(matches(isDisplayed()))
    }

    @Test
    fun testTouchDemonstrationOverlay() {
        val scenario = ActivityScenario.launch<BookReaderActivity>(getStartIntent())
        
        Thread.sleep(2000)

        // Abre as funções de toque via método direto para evitar flakiness de menu
        scenario.onActivity { 
            it.openTouchFunctions() 
        }
        
        Thread.sleep(500)
        
        // Verifica se o container de demonstração de toque apareceu
        onView(withId(R.id.reader_book_container_touch_demonstration)).check(matches(isDisplayed()))
        
        // Verifica se os textos de instrução (ex: TOP, LEFT, etc) estão visíveis no overlay
        onView(withId(R.id.reader_book_touch_top)).check(matches(isDisplayed()))
        
        // Clica para fechar o overlay
        onView(withId(R.id.reader_book_container_touch_demonstration)).perform(click())
        
        Thread.sleep(500)
        
        // Verifica se fechou
        onView(withId(R.id.reader_book_container_touch_demonstration)).check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    @Test
    fun testConfigurationPopupTrigger() {
        ActivityScenario.launch<BookReaderActivity>(getStartIntent())
        
        Thread.sleep(2000)

        // Clica no botão de configurações de fonte no menu
        onView(withId(R.id.menu_item_reader_book_font_style)).perform(click())
        
        Thread.sleep(1000)
        
        // Verifica se o BottomSheet ou SideSheet de configuração apareceu
        // O ID é o container popup_book_configuration_tab
        onView(withId(R.id.popup_book_configuration_tab)).check(matches(isDisplayed()))
    }
}
