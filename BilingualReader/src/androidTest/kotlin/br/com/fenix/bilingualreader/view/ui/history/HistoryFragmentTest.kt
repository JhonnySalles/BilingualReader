package br.com.fenix.bilingualreader.view.ui.history

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions.swipeDown
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import br.com.fenix.bilingualreader.MainActivity
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.enums.FileType
import br.com.fenix.bilingualreader.model.enums.Languages
import br.com.fenix.bilingualreader.model.enums.Libraries
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.repository.DataBase
import org.hamcrest.Matcher
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime
import java.util.Date

@RunWith(AndroidJUnit4::class)
@LargeTest
class HistoryFragmentTest {

    private lateinit var db: DataBase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        // Initialize in-memory database BEFORE activity starts
        db = Room.inMemoryDatabaseBuilder(context, DataBase::class.java)
            .allowMainThreadQueries()
            .build()
        DataBase.setTestingInstance(db)
        
        seedData()
        
        Intents.init()
    }

    private fun launchHistory() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java).apply {
            data = Uri.parse("history")
        }
        ActivityScenario.launch<MainActivity>(intent)
        
        // Wait for fragment transaction and skeleton
        waitForSkeleton()
    }

    private fun seedData() {
        val libraryManga = Library(id = 1L, title = "Manga Lib", path = "/manga", language = Libraries.JAPANESE, type = Type.MANGA)
        val libraryBook = Library(id = 2L, title = "Book Lib", path = "/book", language = Libraries.ENGLISH, type = Type.BOOK)
        
        db.getLibrariesDao().save(libraryManga)
        db.getLibrariesDao().save(libraryBook)
        
        // Insert Mangas (history items must have lastAccess != null)
        for (i in 1..20) {
            val manga = Manga(
                id = i.toLong(),
                title = "Manga $i",
                path = "/manga/manga$i",
                folder = "/manga",
                name = "manga$i",
                fileSize = 1000L,
                fileType = FileType.CBZ,
                pages = 100,
                chapters = intArrayOf(1),
                chaptersPages = mapOf(1 to "Page 1"),
                bookMark = 10,
                completed = false,
                favorite = i % 2 == 0,
                hasSubtitle = true,
                author = "Author $i",
                series = "Series X",
                genre = "Genre Y",
                publisher = "Pub Z",
                volume = "$i",
                release = null,
                fkLibrary = 1L,
                excluded = false,
                dateCreate = LocalDateTime.now().minusDays(10),
                lastAccess = LocalDateTime.now().minusHours(i.toLong()), // Diverse access times
                lastAlteration = LocalDateTime.now(),
                fileAlteration = Date(),
                lastVocabImport = null,
                lastVerify = null
            )
            db.getMangaDao().save(manga)
        }
        
        // Insert Books
        for (i in 1..10) {
            val book = Book(
                id = i.toLong() + 100,
                title = "Book $i",
                author = "Author $i",
                password = "",
                annotation = "",
                release = null,
                genre = "",
                publisher = "",
                series = "",
                isbn = "",
                pages = 200,
                volume = "",
                chapter = 0,
                chapterDescription = "",
                bookMark = 50,
                completed = false,
                language = Languages.ENGLISH,
                path = "/book/book$i",
                folder = "/book",
                name = "book$i",
                fileType = FileType.EPUB,
                fileSize = 5000L,
                favorite = false,
                fkLibrary = 2L,
                tags = mutableListOf(),
                excluded = false,
                dateCreate = LocalDateTime.now().minusDays(5),
                lastAccess = LocalDateTime.now().minusHours(i.toLong()),
                lastAlteration = LocalDateTime.now(),
                fileAlteration = Date(),
                lastVocabImport = null,
                lastVerify = null
            )
            db.getBookDao().save(book)
        }
    }

    @After
    fun tearDown() {
        Intents.release()
        DataBase.close()
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.cacheDir.deleteRecursively()
    }

    @Test
    fun testHistoryFragmentDisplayed() {
        launchHistory()
        waitForViewToBeGone(withId(R.id.shimmer_skeleton))
        waitForView(withId(R.id.history_list))
    }

    @Test
    fun testScrollButtonsVisibility() {
        launchHistory()
        waitForViewToBeGone(withId(R.id.shimmer_skeleton))
        
        // Scroll Up and Down buttons should be hidden initially
        onView(withId(R.id.history_scroll_up)).check(matches(not(isDisplayed())))
        onView(withId(R.id.history_scroll_down)).check(matches(not(isDisplayed())))

        // Scroll down to show Scroll Up button
        onView(withId(R.id.history_list)).perform(swipeUp())
        waitForView(withId(R.id.history_scroll_up))
        
        // Scroll up to show Scroll Down button
        onView(withId(R.id.history_list)).perform(swipeDown())
        waitForView(withId(R.id.history_scroll_down))
    }

    private fun waitForView(viewMatcher: Matcher<View>, timeout: Long = 10000): ViewInteraction {
        val startTime = System.currentTimeMillis()
        val endTime = startTime + timeout

        do {
            try {
                val interaction = onView(viewMatcher)
                interaction.check(matches(isDisplayed()))
                return interaction
            } catch (e: Throwable) {
                Thread.sleep(500)
            }
        } while (System.currentTimeMillis() < endTime)

        return onView(viewMatcher).check(matches(isDisplayed()))
    }

    private fun waitForViewToBeGone(viewMatcher: Matcher<View>, timeout: Long = 10000) {
        val startTime = System.currentTimeMillis()
        val endTime = startTime + timeout

        do {
            try {
                onView(viewMatcher).check(matches(not(isDisplayed())))
                return
            } catch (e: Throwable) {
                if (e is NoMatchingViewException) return
                Thread.sleep(500)
            }
        } while (System.currentTimeMillis() < endTime)
    }

    private fun waitForSkeleton() {
        val startTime = System.currentTimeMillis()
        val timeout = 5000L
        while (System.currentTimeMillis() < startTime + timeout) {
            try {
                onView(withId(R.id.shimmer_skeleton)).check(matches(isDisplayed()))
                return
            } catch (e: Throwable) {
                Thread.sleep(100)
            }
        }
    }
}
