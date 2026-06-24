package br.com.fenix.bilingualreader.view.ui.detail.book

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.HorizontalScrollView
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.entity.Tags
import br.com.fenix.bilingualreader.model.enums.Languages
import br.com.fenix.bilingualreader.model.enums.Libraries
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.repository.DataBase
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.view.ui.detail.DetailActivity
import br.com.fenix.bilingualreader.view.ui.vocabulary.VocabularyActivity
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.isA
import org.junit.After
import org.junit.Assert.assertEquals
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
        val bookFile = File(mockPath, "Detail Test Book.epub")
        if (!bookFile.exists()) bookFile.createNewFile()

        mockBook = Book(mockLib.id, 100L, bookFile).apply {
            title = "Detail Test Book"
            author = "Test Author"
            pages = 100
            bookMark = 50
            favorite = false
            language = Languages.PORTUGUESE
        }
        db.getBookDao().save(mockBook)

        Intents.init()
    }

    @After
    fun tearDown() {
        Intents.release()
        DataBase.close()
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.cacheDir.deleteRecursively()
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
        
        waitForView(withId(R.id.book_detail_title))

        onView(withId(R.id.book_detail_title)).check(matches(withText(mockBook.title)))
        onView(withId(R.id.book_detail_author)).check(matches(withText(mockBook.author)))
        onView(withId(R.id.book_detail_folder)).check(matches(withText(mockBook.path)))
        onView(withId(R.id.book_detail_book_mark)).check(matches(withText(containsString("50 / 100"))))
    }

    @Test
    fun testBookFullInformationDisplay() {
        mockBook.apply {
            publisher = "Editora Alpha"
            isbn = "978-1234567890"
            genre = "Fantasia, Aventura"
        }
        db.getBookDao().update(mockBook)

        ActivityScenario.launch<DetailActivity>(getStartIntent())
        
        // Wait specifically for the publisher text to appear
        waitForView(allOf(withId(R.id.book_detail_information_publish), withText(containsString("Editora Alpha"))))

        onView(withId(R.id.book_detail_information_isbn))
            .perform(scrollTo())
            .check(matches(withText(containsString("978-1234567890"))))

        onView(withId(R.id.book_detail_information_genres))
            .perform(scrollTo())
            .check(matches(withText(containsString("Fantasia, Aventura"))))
    }

    @Test
    fun testFavoriteToggle() {
        ActivityScenario.launch<DetailActivity>(getStartIntent())
        waitForView(withId(R.id.book_detail_button_favorite))

        assertFalse(db.getBookDao().get(mockBook.id!!)!!.favorite)
        onView(withId(R.id.book_detail_button_favorite)).perform(click())
        
        Thread.sleep(500)
        assertTrue(db.getBookDao().get(mockBook.id!!)!!.favorite)
            
        onView(withId(R.id.book_detail_button_favorite)).perform(click())
        Thread.sleep(500)
        assertFalse(db.getBookDao().get(mockBook.id!!)!!.favorite)
    }

    @Test
    fun testVocabularyNavigation() {
        ActivityScenario.launch<DetailActivity>(getStartIntent())
        waitForView(withId(R.id.book_detail_scroll_view))

        // Scroll vertically to the button container
        onView(withId(R.id.book_detail_scroll_view)).perform(scrollTo())
        
        // Scroll horizontally to the end (immediate)
        onView(withId(R.id.book_detail_scroll_view)).perform(scrollHorizontalToRightImmediate())

        // Now click the button which should be visible
        waitForView(withId(R.id.book_detail_button_vocabulary))
        onView(withId(R.id.book_detail_button_vocabulary)).perform(click())

        intended(allOf(
            hasComponent(VocabularyActivity::class.java.name),
            hasExtra(GeneralConsts.KEYS.VOCABULARY.TYPE, Type.BOOK)
        ))
    }

    @Test
    fun testLanguageDropdownChange() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val languages = context.resources.getStringArray(R.array.languages)
        val englishStr = languages[1] // "English"

        ActivityScenario.launch<DetailActivity>(getStartIntent())
        waitForView(withId(R.id.book_detail_information_menu_autocomplete_language))

        onView(withId(R.id.book_detail_information_menu_autocomplete_language)).perform(scrollTo(), click())

        // Wait for the popup and try to click the text directly matching the popup root
        var selected = false
        val endTime = System.currentTimeMillis() + 5000
        while (System.currentTimeMillis() < endTime && !selected) {
            try {
                onView(withText(englishStr))
                    .inRoot(isPlatformPopup())
                    .perform(click())
                selected = true
            } catch (e: Throwable) {
                Thread.sleep(500)
            }
        }

        if (!selected) {
             onData(equalTo(englishStr))
                .inRoot(isPlatformPopup())
                .perform(click())
        }

        Thread.sleep(1000)
        val updatedBook = db.getBookDao().get(mockBook.id!!)!!
        assertEquals(Languages.ENGLISH, updatedBook.language)
    }

    @Test
    fun testTagsDisplay() {
        val mockTag = Tags(1L, "Fantasia", false)
        db.getTagsDao().save(mockTag)
        mockBook.tags = mutableListOf(1L)
        db.getBookDao().update(mockBook)

        ActivityScenario.launch<DetailActivity>(getStartIntent())
        waitForView(withId(R.id.book_detail_information_tags_list))

        onView(withId(R.id.book_detail_information_tags_list))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
            
        onView(withText("Fantasia")).check(matches(isDisplayed()))
    }

    @Test
    fun testCoverPopup() {
        ActivityScenario.launch<DetailActivity>(getStartIntent())
        waitForView(withId(R.id.book_detail_book_image))

        onView(withId(R.id.book_detail_book_image)).perform(click())

        onView(withId(R.id.popup_detail_image))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
    }

    @Test
    fun testDeleteDialogAppearance() {
        ActivityScenario.launch<DetailActivity>(getStartIntent())
        waitForView(withId(R.id.book_detail_scroll_view))

        onView(withId(R.id.book_detail_scroll_view)).perform(scrollTo())
        onView(withId(R.id.book_detail_scroll_view)).perform(scrollHorizontalToRightImmediate())
        
        onView(withId(R.id.book_detail_button_delete)).perform(click())
        
        waitForView(allOf(withText(R.string.book_library_menu_delete), withId(androidx.appcompat.R.id.alertTitle)))
        onView(withText(R.string.action_negative)).perform(click())
        
        Thread.sleep(500)
        onView(allOf(withText(R.string.book_library_menu_delete), withId(androidx.appcompat.R.id.alertTitle))).check(doesNotExist())
    }

    @Test
    fun testMarkReadButton() {
        ActivityScenario.launch<DetailActivity>(getStartIntent())
        waitForView(withId(R.id.book_detail_button_mark_read))

        onView(withId(R.id.book_detail_button_mark_read)).perform(click())
        
        Thread.sleep(500)
        val updatedBook = db.getBookDao().get(mockBook.id!!)!!
        assertEquals(updatedBook.pages, updatedBook.bookMark)
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

    private fun scrollHorizontalToRightImmediate(): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> = allOf(isDisplayed(), isA(HorizontalScrollView::class.java))
            override fun getDescription(): String = "scroll to right immediate"
            override fun perform(uiController: UiController, view: View) {
                (view as HorizontalScrollView).scrollTo(5000, 0)
                uiController.loopMainThreadUntilIdle()
            }
        }
    }
}
