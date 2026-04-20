package br.com.fenix.bilingualreader.view.ui.detail.manga

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.HorizontalScrollView
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.enums.Libraries
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.repository.DataBase
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.view.ui.detail.DetailActivity
import br.com.fenix.bilingualreader.view.ui.vocabulary.VocabularyActivity
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.isA
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class MangaDetailFragmentTest {

    private lateinit var db: DataBase
    private lateinit var mockManga: Manga
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
        val mockPath = File(context.cacheDir, "mock_mangas_detail")
        if (!mockPath.exists()) mockPath.mkdirs()
        
        mockLib = Library(
            id = 1L,
            title = "Test Library Manga",
            path = mockPath.absolutePath,
            language = Libraries.ENGLISH,
            type = Type.MANGA
        )
        db.getLibrariesDao().save(mockLib)

        // Setup Manga
        val mangaDir = File(context.cacheDir, "test_manga_dir")
        if (mangaDir.exists()) mangaDir.deleteRecursively()
        mangaDir.mkdirs()
        
        // Add 4 dummy images to satisfy DirectoryParse.numPages() < 4 check
        for (i in 1..4) {
            File(mangaDir, "page_$i.jpg").writeText("dummy content")
        }
        
        // Add ComicInfo.xml
        val comicInfoFile = File(mangaDir, "ComicInfo.xml")
        comicInfoFile.writeText("""
            <?xml version="1.0"?>
            <ComicInfo xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
              <Title>Detail Test Manga</Title>
              <Series>Série Épica de Teste</Series>
              <Volume>22</Volume>
              <Genre>Isekai, Comedy</Genre>
              <Year>2025</Year>
              <Month>1</Month>
              <Day>1</Day>
              <Writer>Mangaka de Teste</Writer>
              <Publisher>Editora Planeta Manga</Publisher>
            </ComicInfo>
        """.trimIndent())

        mockManga = Manga(mockLib.id, null, mangaDir).apply {
            title = "Detail Test Manga"
            pages = 4
            bookMark = 0
            favorite = false
        }
        mockManga.id = db.getMangaDao().save(mockManga)

        Intents.init()
    }

    @After
    fun tearDown() {
        Intents.release()
        DataBase.setTestingInstance(null)
        db.close()
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.cacheDir.deleteRecursively()
    }

    private fun getStartIntent(): Intent {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return Intent(context, DetailActivity::class.java).apply {
            putExtra(GeneralConsts.KEYS.OBJECT.LIBRARY, mockLib)
            putExtra(GeneralConsts.KEYS.OBJECT.MANGA, mockManga)
        }
    }

    @Test
    fun testMangaDetailDisplay() {
        ActivityScenario.launch<DetailActivity>(getStartIntent())
        
        waitForView(withId(R.id.manga_detail_title))

        onView(withId(R.id.manga_detail_title)).check(matches(withText(mockManga.name)))
        onView(withId(R.id.manga_detail_folder)).check(matches(withText(mockManga.path)))
        
        // Wait for the progress view to be populated and visible
        waitForView(withId(R.id.manga_detail_book_mark))
    }

    @Test
    fun testMangaFullInformationDisplay() {
        mockManga = db.getMangaDao().get(mockManga.id!!)!!
        mockManga.apply {
            fkLibrary = mockLib.id
            author = "Mangaka de Teste"
            series = "Série Épica de Teste"
            volume = "22"
            publisher = "Editora Planeta Manga"
            genre = "Seinen, Psicológico"
            release = LocalDate.of(2025, 1, 1)
        }
        db.getMangaDao().update(mockManga)
        
        // Double check DB state
        val verify = db.getMangaDao().get(mockManga.id!!)!!
        assertEquals("22", verify.volume)
        assertEquals(mockLib.id, verify.fkLibrary)

        ActivityScenario.launch<DetailActivity>(getStartIntent())
        
        // Wait for the metadata section to become visible
        waitForView(withId(R.id.manga_detail_local_information_title))

        // Wait for content specifically
        waitForText(withId(R.id.manga_detail_local_information_authors), "Mangaka de Teste")

        onView(withId(R.id.manga_detail_local_information_series))
            .perform(betterScrollTo())
        waitForText(withId(R.id.manga_detail_local_information_series), "Série Épica de Teste")

        onView(withId(R.id.manga_detail_local_information_volume))
            .perform(betterScrollTo())
        waitForText(withId(R.id.manga_detail_local_information_volume), "22")

        onView(withId(R.id.manga_detail_local_information_publisher))
            .perform(betterScrollTo())
        waitForText(withId(R.id.manga_detail_local_information_publisher), "Editora Planeta Manga")

        onView(withId(R.id.manga_detail_local_information_release))
            .perform(betterScrollTo())
        waitForText(withId(R.id.manga_detail_local_information_release), "2025")
    }

    @Test
    fun testFavoriteToggle() {
        ActivityScenario.launch<DetailActivity>(getStartIntent())
        waitForView(withId(R.id.manga_detail_button_favorite))

        assertFalse(db.getMangaDao().get(mockManga.id!!)!!.favorite)
        onView(withId(R.id.manga_detail_button_favorite)).perform(click())
        
        Thread.sleep(6000) // Even longer
        assertTrue(db.getMangaDao().get(mockManga.id!!)!!.favorite)
            
        onView(withId(R.id.manga_detail_button_favorite)).perform(click())
        Thread.sleep(6000)
        assertFalse(db.getMangaDao().get(mockManga.id!!)!!.favorite)
    }

    @Test
    fun testDeleteDialogAppearance() {
        ActivityScenario.launch<DetailActivity>(getStartIntent())
        waitForView(withId(R.id.manga_detail_scroll_view))

        onView(withId(R.id.manga_detail_scroll_view)).perform(betterScrollTo())
        onView(withId(R.id.manga_detail_scroll_view)).perform(scrollHorizontalToRightImmediate())
        
        onView(withId(R.id.manga_detail_button_delete)).perform(click())
        
        waitForView(allOf(withText(R.string.manga_library_menu_delete), withId(androidx.appcompat.R.id.alertTitle)))
        onView(withText(R.string.action_negative)).inRoot(isDialog()).perform(click())
        
        // Wait for dialog to disappear
        var dismissed = false
        val timeout = System.currentTimeMillis() + 10000
        while (System.currentTimeMillis() < timeout && !dismissed) {
            try {
                onView(allOf(withText(R.string.manga_library_menu_delete), withId(androidx.appcompat.R.id.alertTitle)))
                    .check(doesNotExist())
                dismissed = true
            } catch (e: Throwable) {
                Thread.sleep(500)
            }
        }
        assertTrue("Dialog should be dismissed", dismissed)
    }

    @Test
    fun testMarkReadButton() {
        ActivityScenario.launch<DetailActivity>(getStartIntent())
        waitForView(withId(R.id.manga_detail_button_mark_read))

        onView(withId(R.id.manga_detail_button_mark_read)).perform(betterScrollTo(), click())
        
        Thread.sleep(6000)
        val updatedManga = db.getMangaDao().get(mockManga.id!!)!!
        assertEquals(updatedManga.pages, updatedManga.bookMark)
    }

    @Test
    fun testVocabularyNavigation() {
        ActivityScenario.launch<DetailActivity>(getStartIntent())
        waitForView(withId(R.id.manga_detail_scroll_view))

        onView(withId(R.id.manga_detail_scroll_view)).perform(betterScrollTo())
        onView(withId(R.id.manga_detail_scroll_view)).perform(scrollHorizontalToRightImmediate())

        onView(withId(R.id.manga_detail_button_vocabulary)).perform(click())

        intended(allOf(
            hasComponent(VocabularyActivity::class.java.name),
            hasExtra(GeneralConsts.KEYS.VOCABULARY.TYPE, Type.MANGA)
        ))
    }

    @Test
    fun testComicInfoTagsDisplay() {
        mockManga = db.getMangaDao().get(mockManga.id!!)!!
        mockManga.apply {
            fkLibrary = mockLib.id
            genre = "Isekai, Comedy"
        }
        db.getMangaDao().update(mockManga)
        
        ActivityScenario.launch<DetailActivity>(getStartIntent())
        
        // Wait for the container first
        waitForView(withId(R.id.manga_detail_local_information_comic_info_tags))
        onView(withId(R.id.manga_detail_local_information_comic_info_tags)).perform(betterScrollTo())

        // Now robustly wait for the tag content to be rendered
        var found = false
        for (i in 1..80) { // EXTREME wait
            try {
                onView(withText("Isekai")).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
                found = true
                break
            } catch (e: Throwable) {
                Thread.sleep(500)
            }
        }
        assertTrue("Tag 'Isekai' should be visible", found)
    }

    @Test
    fun testCoverPopup() {
        ActivityScenario.launch<DetailActivity>(getStartIntent())
        waitForView(withId(R.id.manga_detail_manga_image))

        onView(withId(R.id.manga_detail_manga_image)).perform(click())

        onView(withId(R.id.popup_detail_image))
            .inRoot(isDialog())
            .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
    }

    private fun waitForText(viewMatcher: Matcher<View>, expectedText: String, timeout: Long = 40000) {
        val endTime = System.currentTimeMillis() + timeout
        while (System.currentTimeMillis() < endTime) {
            try {
                onView(viewMatcher).check(matches(withText(containsString(expectedText))))
                return
            } catch (e: Throwable) {
                Thread.sleep(1000)
            }
        }
        onView(viewMatcher).check(matches(withText(containsString(expectedText))))
    }

    private fun waitForView(viewMatcher: Matcher<View>, timeout: Long = 60000): ViewInteraction {
        val startTime = System.currentTimeMillis()
        val endTime = startTime + timeout

        do {
            try {
                val interaction = onView(viewMatcher)
                interaction.check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
                return interaction
            } catch (e: Throwable) {
                Thread.sleep(1000)
            }
        } while (System.currentTimeMillis() < endTime)

        return onView(viewMatcher).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
    }

    private fun betterScrollTo(): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> = isAssignableFrom(View::class.java)
            override fun getDescription(): String = "better scroll to"
            override fun perform(uiController: UiController, view: View) {
                var current: View? = view
                val viewRect = android.graphics.Rect()
                view.getDrawingRect(viewRect)

                while (current != null) {
                    val p = current.parent as? android.view.ViewGroup ?: break
                    if (p is androidx.core.widget.NestedScrollView || p is android.widget.ScrollView) {
                        val mappedRect = android.graphics.Rect(viewRect)
                        p.offsetDescendantRectToMyCoords(view, mappedRect)
                        p.scrollTo(0, mappedRect.top)
                        uiController.loopMainThreadUntilIdle()
                    }
                    current = p
                }
                view.requestFocus()
                uiController.loopMainThreadUntilIdle()
                uiController.loopMainThreadForAtLeast(2000) // Ensure UI settled
            }
        }
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
