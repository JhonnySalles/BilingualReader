package br.com.fenix.bilingualreader.view.ui.reader.book

import android.content.Context
import android.content.Intent
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.enums.Libraries
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.parses.book.DocumentParse
import br.com.fenix.bilingualreader.service.repository.DataBase
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.Telemetry
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import android.view.View

@RunWith(AndroidJUnit4::class)
class BookReaderActivityTest {

    private lateinit var db: DataBase
    private lateinit var mockBook: Book
    private lateinit var mockLib: Library

    @Before
    fun setup() {
        val appContext = ApplicationProvider.getApplicationContext<Context>()
        val testContext = InstrumentationRegistry.getInstrumentation().context
        
        Telemetry.isEnabled = false
        
        // Initialize DocumentParse for native libraries and cache
        DocumentParse.init(appContext)
        
        db = Room.inMemoryDatabaseBuilder(appContext, DataBase::class.java)
            .allowMainThreadQueries()
            .build()
        DataBase.setTestingInstance(db)

        GeneralConsts.getSharedPreferences(appContext).edit()
            .putBoolean(GeneralConsts.KEYS.TOUCH.BOOK_TOUCH_DEMONSTRATION, false)
            .commit()

        val mockPath = File(appContext.cacheDir, "mock_books_reader")
        if (mockPath.exists()) mockPath.deleteRecursively()
        mockPath.mkdirs()
        
        mockLib = Library(id = 1L, title = "Book Test Library", path = mockPath.absolutePath, language = Libraries.ENGLISH, type = Type.BOOK)
        db.getLibrariesDao().save(mockLib)

        val bookFile = File(mockPath, "book_test.epub")
        testContext.assets.open("livro.epub").use { input ->
            FileOutputStream(bookFile).use { output ->
                input.copyTo(output)
            }
        }

        mockBook = Book(mockLib.id, 500L, bookFile).apply {
            title = "Reader UI Test Book"
            author = "Test Author"
            pages = 10 
            bookMark = 1
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
            action = Intent.ACTION_MAIN
        }
    }

    @Test
    fun testHUDMetadataDisplay() {
        ActivityScenario.launch<BookReaderActivity>(getStartIntent()).use { scenario ->
            Thread.sleep(15000) 

            // Forçamos o estado de Fullscreen no fragment para garantir que os componentes HUD apareçam
            scenario.onActivity { activity ->
                val fragment = activity.supportFragmentManager.findFragmentById(R.id.root_frame_book_reader) as? BookReaderFragment
                fragment?.setFullscreen(false)
            }
            Thread.sleep(3000)

            onView(withId(R.id.reader_book_toolbar_top)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            onView(withId(R.id.reader_book_toolbar_title)).check(matches(withText(mockBook.name)))
            onView(withId(R.id.reader_book_toolbar_bottom_author)).check(matches(withText(mockBook.author)))
        }
    }

    @Test
    fun testConfigurationPopupTrigger() {
        ActivityScenario.launch<BookReaderActivity>(getStartIntent()).use { scenario ->
            Thread.sleep(15000)

            scenario.onActivity { activity ->
                val fragment = activity.supportFragmentManager.findFragmentById(R.id.root_frame_book_reader) as? BookReaderFragment
                fragment?.setFullscreen(false)
            }
            Thread.sleep(3000)

            // Ativa o comando de abrir popup através da Activity
            scenario.onActivity { activity ->
                val toolbar = activity.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar_book_reader)
                activity.onOptionsItemSelected(toolbar.menu.findItem(R.id.menu_item_reader_book_font_style))
            }
            
            Thread.sleep(8000) 
            
            onView(withId(R.id.popup_book_configuration_tab)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        }
    }
}
