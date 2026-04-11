package br.com.fenix.bilingualreader.view.ui.history

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.action.ViewActions.pressImeActionButton
import androidx.test.espresso.action.ViewActions.swipeLeft
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.TestActivity
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.enums.Libraries
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.repository.DataBase
import org.hamcrest.Matchers.containsString
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class HistoryFragmentTest {

    private lateinit var db: DataBase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, DataBase::class.java)
            .allowMainThreadQueries()
            .build()
        
        // Injeta o banco de dados em memória no Singleton conforme o modelo existente
        DataBase.setTestingInstance(db)

        val mangaLibId = 100L
        val bookLibId = 200L

        // Configura bibliotecas de teste
        db.getLibrariesDao().save(Library(mangaLibId, "Manga Test Lib", "/mock/manga", Libraries.JAPANESE, Type.MANGA))
        db.getLibrariesDao().save(Library(bookLibId, "Book Test Lib", "/mock/book", Libraries.ENGLISH, Type.BOOK))

        // Popula com 5 Mangás e 5 Livros (Total 10 itens como solicitado)
        // Definimos datas de acesso decrescentes para que a ordenação seja previsível (os mais recentes primeiro)
        for (i in 1..5) {
            val manga = Manga(mangaLibId, i.toLong(), File("/mock/manga/manga$i.cbz")).apply {
                title = "Manga Alpha $i"
                author = "Author Manga $i"
                lastAccess = LocalDateTime.now().minusDays(i.toLong())
                excluded = false
            }
            db.getMangaDao().save(manga)

            val book = Book(bookLibId, (i + 10).toLong(), File("/mock/book/book$i.epub")).apply {
                title = "Book Beta $i"
                author = "Author Book $i"
                lastAccess = LocalDateTime.now().minusHours(i.toLong())
                excluded = false
            }
            db.getBookDao().save(book)
        }
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testHistoryListIsDisplayed() {
        val scenario = ActivityScenario.launch(TestActivity::class.java)
        scenario.onActivity { activity ->
            activity.setFragment(HistoryFragment())
        }
        
        Thread.sleep(2000)
        
        onView(withId(R.id.history_list)).check(matches(isDisplayed()))
        onView(withText("Book Beta 1")).check(matches(isDisplayed()))
        onView(withText("Manga Alpha 1")).check(matches(isDisplayed()))
    }

    @Test
    fun testFilterByType() {
        val scenario = ActivityScenario.launch(TestActivity::class.java)
        scenario.onActivity { activity ->
            activity.setFragment(HistoryFragment())
        }
        
        Thread.sleep(2000)

        onView(withId(R.id.menu_history_type)).perform(click())
        onView(withText(R.string.history_manga)).perform(click())
        
        Thread.sleep(1000)

        onView(withText("Manga Alpha 1")).check(matches(isDisplayed()))
        onView(withText("Book Beta 1")).check(doesNotExist())
    }

    @Test
    fun testSwipeToDeletePrompt() {
        val scenario = ActivityScenario.launch(TestActivity::class.java)
        scenario.onActivity { activity ->
            activity.setFragment(HistoryFragment())
        }
        
        Thread.sleep(2000)

        onView(withId(R.id.history_list))
            .perform(RecyclerViewActions.actionOnItemAtPosition<androidx.recyclerview.widget.RecyclerView.ViewHolder>(0, swipeLeft()))

        Thread.sleep(1000)

        onView(withText(R.string.manga_library_menu_delete)).check(matches(isDisplayed()))
        onView(withText(containsString("Book Beta 1"))).check(matches(isDisplayed()))
        
        onView(withText(R.string.action_negative)).perform(click())
        onView(withText("Book Beta 1")).check(matches(isDisplayed()))
    }

    @Test
    fun testSearchFiltering() {
        val scenario = ActivityScenario.launch(TestActivity::class.java)
        scenario.onActivity { activity ->
            activity.setFragment(HistoryFragment())
        }
        
        Thread.sleep(2000)

        onView(withId(R.id.menu_history_search)).perform(click())
        onView(isAssignableFrom(android.widget.EditText::class.java))
            .perform(typeText("Book Beta 1"), pressImeActionButton())

        Thread.sleep(2000)

        onView(withText("Book Beta 1")).check(matches(isDisplayed()))
        onView(withText("Manga Alpha 1")).check(doesNotExist())
    }

    @Test
    fun testLibraryFilter() {
        val scenario = ActivityScenario.launch(TestActivity::class.java)
        scenario.onActivity { activity ->
            activity.setFragment(HistoryFragment())
        }
        
        Thread.sleep(2000)

        onView(withId(R.id.menu_history_library)).perform(click())
        onView(withText(R.string.history_manga)).perform(click())
        onView(withText("Manga Test Lib")).perform(click())

        Thread.sleep(2000)

        onView(withText("Manga Alpha 1")).check(matches(isDisplayed()))
        onView(withText("Book Beta 1")).check(doesNotExist())
    }

    @Test
    fun testLongClickContextMenu() {
        val scenario = ActivityScenario.launch(TestActivity::class.java)
        scenario.onActivity { activity ->
            activity.setFragment(HistoryFragment())
        }
        
        Thread.sleep(2000)

        onView(withId(R.id.history_list))
            .perform(RecyclerViewActions.actionOnItemAtPosition<androidx.recyclerview.widget.RecyclerView.ViewHolder>(0, longClick()))

        onView(withText(R.string.book_library_menu_favorite_add)).check(matches(isDisplayed()))
        onView(withText(R.string.manga_library_menu_clear)).check(matches(isDisplayed()))
    }

    @Test
    fun testFabVisibilityOnScroll() {
        val scenario = ActivityScenario.launch(TestActivity::class.java)
        scenario.onActivity { activity ->
            activity.setFragment(HistoryFragment())
        }
        
        Thread.sleep(2000)

        onView(withId(R.id.history_scroll_up)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.history_list)).perform(RecyclerViewActions.scrollToPosition<androidx.recyclerview.widget.RecyclerView.ViewHolder>(9))

        Thread.sleep(1000)

        onView(withId(R.id.history_scroll_up)).check(matches(isDisplayed()))
        onView(withId(R.id.history_scroll_up)).perform(click())
        
        Thread.sleep(1000)
        
        onView(withText("Book Beta 1")).check(matches(isDisplayed()))
    }
}

