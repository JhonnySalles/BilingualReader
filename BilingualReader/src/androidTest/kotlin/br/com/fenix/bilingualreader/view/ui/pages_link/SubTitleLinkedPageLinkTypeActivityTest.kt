package br.com.fenix.bilingualreader.view.ui.pages_link

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.utils.MangaTestUtil
import com.google.android.material.progressindicator.CircularProgressIndicator
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4::class)
class SubTitleLinkedPageLinkTypeActivityTest {

    // Inform a file test here
    private val filePath = "/storage/emulated/0/Mangas/Akame ga Kill - Volume 01.cbr" // "/storage/emulated/0/Manga/Manga of test.cbr"
    private val filePathLink = filePath
    private val manga: Manga = MangaTestUtil.getManga(ApplicationProvider.getApplicationContext(), filePath, 1)
    private var intent: Intent? = null

    init {
        assertFalse("Not informed comic file, please declare 'filePath' in SubTitleLinkedPageLinkTypeActivityTest", filePath.isEmpty())
        assertTrue("Comic file informed not found, please verify declared 'filePath' in SubTitleLinkedPageLinkTypeActivityTest", manga.file.exists())

        intent = Intent(ApplicationProvider.getApplicationContext(), PagesLinkActivity::class.java)

        val bundle = Bundle()
        bundle.putSerializable(GeneralConsts.KEYS.OBJECT.LIBRARY, manga.library)
        bundle.putSerializable(GeneralConsts.KEYS.OBJECT.MANGA, manga)
        bundle.putInt(GeneralConsts.KEYS.MANGA.PAGE_NUMBER, 0)
        intent?.putExtras(bundle)
    }


    @get:Rule
    val activityScenarioRule = ActivityScenarioRule<PagesLinkActivity>(intent)

    private val awaitProcessSeconds = 2L

    @Test
    fun `1_test_page_link`() {
        val waiter = CountDownLatch(1)
        val scenario = activityScenarioRule.scenario

        scenario.onActivity {
            val fragment = it.supportFragmentManager.findFragmentById(R.id.root_frame_pages_link)
            assertTrue(fragment is PagesLinkFragment)
        }

        var progress: CircularProgressIndicator? = null

        scenario.onActivity {
            val fragment = it.supportFragmentManager.findFragmentById(R.id.root_frame_pages_link) as PagesLinkFragment

            val resultData = Intent()
            resultData.data = filePathLink.toUri()
            fragment.onActivityResult(GeneralConsts.REQUEST.OPEN_PAGE_LINK, Activity.RESULT_OK, resultData)
            progress = it.findViewById(R.id.pages_link_loading_progress)
        }

        // Await load manga images
        if (progress != null) {
            var waitTime = 0
            do {
                waitTime++
                waiter.await(2, TimeUnit.SECONDS)
            } while (progress!!.visibility == View.VISIBLE && waitTime < 5)
        }

        waiter.await(awaitProcessSeconds, TimeUnit.SECONDS)
        onView(withId(R.id.pages_link_help_button)).perform(click())

        waiter.await(awaitProcessSeconds, TimeUnit.SECONDS)
        onView(withId(R.id.pages_link_full_screen_button)).perform(click())

        waiter.await(awaitProcessSeconds, TimeUnit.SECONDS)
        onView(withId(R.id.pages_link_full_screen_button)).perform(click())

        waiter.await(awaitProcessSeconds, TimeUnit.SECONDS)
        onView(withId(R.id.pages_link_dual_page_button)).perform(click())

        waiter.await(awaitProcessSeconds, TimeUnit.SECONDS)
        onView(withId(R.id.pages_link_single_page_button)).perform(click())

        waiter.await(awaitProcessSeconds, TimeUnit.SECONDS)
        onView(withId(R.id.pages_link_reorder_button)).perform(click())

        waiter.await(awaitProcessSeconds, TimeUnit.SECONDS)
        onView(withId(R.id.pages_link_auto_process_button)).perform(click())

        waiter.await(awaitProcessSeconds, TimeUnit.SECONDS)
        onView(withId(R.id.pages_link_reload_button)).perform(click())

        waiter.await(awaitProcessSeconds, TimeUnit.SECONDS)
        onView(withId(R.id.file_link_delete_button)).perform(click())

        scenario.onActivity {
            val fragment = it.supportFragmentManager.findFragmentById(R.id.root_frame_pages_link) as PagesLinkFragment

            val resultData = Intent()
            resultData.data = filePathLink.toUri()
            fragment.onActivityResult(GeneralConsts.REQUEST.OPEN_PAGE_LINK, Activity.RESULT_OK, resultData)
        }

        // Has a error, view another day
        waiter.await(awaitProcessSeconds, TimeUnit.SECONDS)
        onView(withId(R.id.pages_link_save_button)).perform(click())

    }
}