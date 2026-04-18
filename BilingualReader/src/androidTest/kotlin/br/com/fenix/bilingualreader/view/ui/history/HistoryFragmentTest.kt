package br.com.fenix.bilingualreader.view.ui.history

import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.content.res.Resources
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.TestActivity
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.enums.FileType
import br.com.fenix.bilingualreader.model.enums.Libraries
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.repository.DataBase
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.view.ui.reader.book.BookReaderActivity
import br.com.fenix.bilingualreader.view.ui.reader.manga.MangaReaderActivity
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.anyOf
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
    fun setup() {
        Intents.init()
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, DataBase::class.java)
            .allowMainThreadQueries()
            .build()
        
        DataBase.setTestingInstance(db)

        val mangaLibId = 100L
        val bookLibId = 200L

        db.getLibrariesDao().save(Library(mangaLibId, "Manga Test Lib", "/mock/manga", Libraries.JAPANESE, Type.MANGA))
        db.getLibrariesDao().save(Library(bookLibId, "Book Test Lib", "/mock/book", Libraries.ENGLISH, Type.BOOK))

        for (i in 1..5) {
            val manga = Manga(mangaLibId, i.toLong(), File(context.cacheDir, "manga$i.cbz")).apply {
                title = "Manga Alpha $i"
                author = "Author Manga $i"
                lastAccess = LocalDateTime.now().minusDays(i.toLong())
                excluded = false
            }
            if (!manga.file.exists()) manga.file.createNewFile()
            db.getMangaDao().save(manga)

            val book = Book(bookLibId, (i + 10).toLong(), File(context.cacheDir, "book$i.epub")).apply {
                title = "Book Beta $i"
                author = "Author Book $i"
                lastAccess = LocalDateTime.now().minusHours(i.toLong())
                excluded = false
            }
            if (!book.file.exists()) book.file.createNewFile()
            db.getBookDao().save(book)
        }
    }

    @After
    fun tearDown() {
        Intents.release()
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.cacheDir.deleteRecursively()
        db.close()
    }

    private fun launchFragment() {
        val scenario = ActivityScenario.launch(TestActivity::class.java)
        scenario.onActivity { activity ->
            activity.setFragment(HistoryFragment())
        }
        waitForSkeleton()
        waitForView(withId(R.id.history_list))
    }

    private fun waitForView(matcher: Matcher<View>, timeout: Long = 5000): ViewInteraction {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() < startTime + timeout) {
            try {
                val interaction = onView(matcher)
                interaction.check(matches(isDisplayed()))
                return interaction
            } catch (e: Throwable) {
                Thread.sleep(100)
            }
        }
        return onView(matcher).check(matches(isDisplayed()))
    }

    private fun waitForSkeleton() {
        val startTime = System.currentTimeMillis()
        val timeout = 5000L
        while (System.currentTimeMillis() < startTime + timeout) {
            try {
                onView(withId(R.id.shimmer_skeleton)).check(matches(withEffectiveVisibility(Visibility.GONE)))
                return
            } catch (e: Throwable) {
                Thread.sleep(100)
            }
        }
    }

    @Test
    fun testHistoryListIsDisplayed() {
        launchFragment()
        onView(allOf(withId(R.id.history_text_title), withText("Book Beta 1"))).check(matches(isDisplayed()))
        onView(allOf(withId(R.id.history_text_title), withText("Manga Alpha 1"))).check(matches(isDisplayed()))
    }

    @Test
    fun testFilterByType() {
        launchFragment()
        onView(withId(R.id.menu_history_type)).perform(click())
        onView(withText(R.string.history_manga)).perform(click())
        
        waitForView(allOf(withId(R.id.history_text_title), withText("Manga Alpha 1")))
        onView(withText("Book Beta 1")).check(doesNotExist())
    }

    @Test
    fun testSearchFiltering() {
        launchFragment()
        onView(withId(R.id.menu_history_search)).perform(click())
        onView(withId(Resources.getSystem().getIdentifier("search_src_text", "id", "android")))
            .perform(typeText("Book Beta 1"), pressImeActionButton())

        waitForView(allOf(withId(R.id.history_text_title), withText("Book Beta 1")))
        onView(withText("Manga Alpha 1")).check(doesNotExist())
    }

    @Test
    fun testSearchWithTags() {
        launchFragment()
        onView(withId(R.id.menu_history_search)).perform(click())
        onView(withId(Resources.getSystem().getIdentifier("search_src_text", "id", "android")))
            .perform(typeText("@Author:\"Author Manga 2\""), pressImeActionButton())

        waitForView(allOf(withId(R.id.history_text_title), withText("Manga Alpha 2")))
        onView(withText("Manga Alpha 1")).check(doesNotExist())
    }

    @Test
    fun testFilterByLibrary() {
        launchFragment()
        onView(withId(R.id.menu_history_library)).perform(click())
        onView(withText(R.string.history_manga)).perform(click())
        onView(withText("Manga Test Lib")).perform(click())

        waitForView(allOf(withId(R.id.history_text_title), withText("Manga Alpha 1")))
        onView(withText("Book Beta 1")).check(doesNotExist())
    }

    @Test
    fun testItemClickNavigatesToReader() {
        launchFragment()
        intending(hasComponent(BookReaderActivity::class.java.name))
            .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))

        onView(allOf(withId(R.id.history_text_title), withText("Book Beta 1"))).perform(click())

        intended(allOf(
            hasComponent(BookReaderActivity::class.java.name),
            hasExtra(GeneralConsts.KEYS.BOOK.NAME, "Book Beta 1")
        ))
    }

    @Test
    fun testLongClickContextMenu() {
        launchFragment()
        onView(withId(R.id.history_list))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, longClick()))

        waitForView(anyOf(withText(R.string.book_library_menu_favorite_add), withText(R.string.manga_library_menu_favorite_add)))
        onView(anyOf(withText(R.string.manga_library_menu_clear), withText(R.string.book_library_menu_clear))).check(matches(isDisplayed()))
    }

    @Test
    fun testItemMenuClearProgress() {
        launchFragment()
        // Posição 1 pular header de data
        onView(withId(R.id.history_list))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, longClick()))

        waitForView(anyOf(withText(R.string.manga_library_menu_clear), withText(R.string.book_library_menu_clear))).perform(click())

        // Verifica se o item sumiu do histórico (porque limpou o lastAccess/progress no ViewModel)
        onView(withText("Book Beta 1")).check(doesNotExist())
    }

    @Test
    fun testSwipeToDeletePrompt() {
        launchFragment()
        onView(withId(R.id.history_list))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, swipeLeft()))

        waitForView(allOf(withText(R.string.manga_library_menu_delete), withId(androidx.appcompat.R.id.alertTitle)))
        onView(withId(android.R.id.button1)).perform(click()) // Confirmar delete
        
        onView(withText("Book Beta 1")).check(doesNotExist())
    }

    @Test
    fun testSwipeToDeleteUndoOnDismiss() {
        launchFragment()
        onView(withId(R.id.history_list))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, swipeLeft()))

        waitForView(allOf(withText(R.string.manga_library_menu_delete), withId(androidx.appcompat.R.id.alertTitle)))
        pressBack() // Cancela o diálogo
        
        waitForView(allOf(withId(R.id.history_text_title), withText("Book Beta 1")))
    }

    @Test
    fun testFileNotFoundShowsDialog() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manga = Manga(100L, 999L, File(context.cacheDir, "non_existent.cbz")).apply {
            title = "Missing Content"
            lastAccess = LocalDateTime.now().plusMinutes(1)
            excluded = false
        }
        db.getMangaDao().save(manga)

        launchFragment()
        waitForView(withText("Missing Content")).perform(click())

        waitForView(withText(R.string.manga_excluded))
    }

    @Test
    fun testScrollUpButtonAppearsOnScroll() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Adiciona muitos itens para permitir scroll
        for (i in 50..80) {
            db.getMangaDao().save(Manga(100L, i.toLong(), File(context.cacheDir, "scroll$i.cbz")).apply {
                title = "Scroll Manga $i"
                lastAccess = LocalDateTime.now().minusDays(i.toLong())
            })
        }

        launchFragment()
        onView(withId(R.id.history_list))
            .perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(20))

        onView(withId(R.id.history_list)).perform(swipeUp())
        
        Thread.sleep(500)
        waitForView(withId(R.id.history_scroll_up))
    }
}
