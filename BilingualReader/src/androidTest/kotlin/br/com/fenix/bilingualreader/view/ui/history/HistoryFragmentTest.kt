package br.com.fenix.bilingualreader.view.ui.history

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import br.com.fenix.bilingualreader.MainActivity
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.service.repository.BookRepository
import br.com.fenix.bilingualreader.service.repository.MangaRepository
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.unmockkAll
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class HistoryFragmentTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @MockK
    lateinit var mMangaRepository: MangaRepository

    @MockK
    lateinit var mBookRepository: BookRepository

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Intents.init()
        
        // Mocking responses to avoid delay or null issues during setup
        every { mMangaRepository.listHistory() } returns arrayListOf()
        every { mBookRepository.listHistory() } returns arrayListOf()

        // Navigate to HistoryFragment
        // Open drawer first
        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open())
        
        // Note: The ID was found in main_menu_drawer.xml as menu_history
        onView(withId(R.id.nav_view)).perform(click()) // Ensure focus on nav view
        onView(withId(R.id.menu_history)).perform(click())
        
        // Wait for fragment transaction
        Thread.sleep(1000)
    }

    @After
    fun tearDown() {
        unmockkAll()
        Intents.release()
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.cacheDir.deleteRecursively()
    }

    @Test
    fun testHistoryFragmentDisplayed() {
        waitForSkeleton()
        waitForViewToBeGone(withId(R.id.shimmer_skeleton))
        waitForView(withId(R.id.history_list))
    }

    @Test
    fun testScrollButtonsVisibility() {
        waitForSkeleton()
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
            } catch (e: Exception) {
                Thread.sleep(500)
            }
        } while (System.currentTimeMillis() < endTime)

        return onView(viewMatcher).check(matches(isDisplayed()))
    }

    private fun waitForViewToBeGone(viewMatcher: Matcher<View>, timeout: Long = 5000) {
        val startTime = System.currentTimeMillis()
        val endTime = startTime + timeout

        do {
            try {
                onView(viewMatcher).check(matches(not(isDisplayed())))
                return
            } catch (e: Exception) {
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
