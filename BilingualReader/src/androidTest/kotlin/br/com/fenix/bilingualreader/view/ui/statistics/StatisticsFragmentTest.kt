package br.com.fenix.bilingualreader.view.ui.statistics

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.repository.DataBase
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.view.ui.menu.MenuActivity
import org.hamcrest.Matchers.containsString
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class StatisticsFragmentTest {

    private lateinit var db: DataBase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        // Setup Database
        db = Room.inMemoryDatabaseBuilder(context, DataBase::class.java)
            .allowMainThreadQueries()
            .build()
        DataBase.setTestingInstance(db)

        // Setup Libraries
        val mangaLib = Library(null).apply {
            title = "Manga Lib"
            path = "path1"
            language = br.com.fenix.bilingualreader.model.enums.Libraries.JAPANESE
            type = Type.MANGA
        }
        val bookLib = Library(null).apply {
            title = "Book Lib"
            path = "path2"
            language = br.com.fenix.bilingualreader.model.enums.Libraries.ENGLISH
            type = Type.BOOK
        }
        val mangaLibId = db.getLibrariesDao().save(mangaLib)
        val bookLibId = db.getLibrariesDao().save(bookLib)

        // Setup Manga data for Statistics
        // 5 Reading
        for (i in 1..5) {
            db.getMangaDao().save(Manga(fkLibrary = mangaLibId, id = null, file = File("manga_reading_$i.cbz")).apply {
                title = "Manga Reading $i"
                pages = 100
                bookMark = 50
            })
        }
        // 10 To Read
        for (i in 1..10) {
            db.getMangaDao().save(Manga(fkLibrary = mangaLibId, id = null, file = File("manga_to_read_$i.cbz")).apply {
                title = "Manga To Read $i"
                pages = 100
                bookMark = 0
            })
        }
        // 3 Read
        for (i in 1..3) {
            db.getMangaDao().save(Manga(fkLibrary = mangaLibId, id = null, file = File("manga_read_$i.cbz")).apply {
                title = "Manga Read $i"
                pages = 100
                bookMark = 100
            })
        }

        // Setup Book data for Statistics
        // 2 Reading
        for (i in 1..2) {
            db.getBookDao().save(Book(fkLibrary = bookLibId, id = null, file = File("book_reading_$i.epub")).apply {
                title = "Book Reading $i"
                pages = 100
                bookMark = 50
            })
        }
        // 5 To Read
        for (i in 1..5) {
            db.getBookDao().save(Book(fkLibrary = bookLibId, id = null, file = File("book_to_read_$i.epub")).apply {
                title = "Book To Read $i"
                pages = 100
                bookMark = 0
            })
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun getStartIntent(): Intent {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = Intent(context, MenuActivity::class.java)
        val bundle = Bundle()
        // Use a valid ID from MenuActivity, or R.id.menu_statistics if it was added.
        // Since we can't change MenuActivity, we'll use one of the existing frame IDs if possible, 
        // but the test specifically tests Statistics, so we might need the correct ID.
        // Assuming R.id.menu_statistics is the intended one after an update.
        bundle.putInt(GeneralConsts.KEYS.FRAGMENT.ID, R.id.menu_statistics)
        intent.putExtras(bundle)
        return intent
    }

    @Test
    fun testStatisticsRendering() {
        ActivityScenario.launch<MenuActivity>(getStartIntent())
        
        // Aguarda carregamento do BlurView e Repository
        Thread.sleep(3000)

        // Verifica renderização dos dados de Mangá
        onView(withId(R.id.statistics_manga_reading)).check(matches(withText("5")))
        onView(withId(R.id.statistics_manga_to_read)).check(matches(withText("10")))
        onView(withId(R.id.statistics_manga_read)).check(matches(withText("3")))
        
        // Verifica cálculo de tempo (1h -> "1h")
        // No código: generateSeconds(3600) -> "1h " (com espaço ou sem dependendo do R.string)
        onView(withId(R.id.statistics_manga_completed_time)).check(matches(withText(containsString("1h"))))

        // Verifica renderização dos dados de Livro
        onView(withId(R.id.statistics_book_reading)).check(matches(withText("2")))
        onView(withId(R.id.statistics_book_to_read)).check(matches(withText("5")))
        onView(withId(R.id.statistics_book_completed_time)).check(matches(withText(containsString("2h"))))
    }
}
