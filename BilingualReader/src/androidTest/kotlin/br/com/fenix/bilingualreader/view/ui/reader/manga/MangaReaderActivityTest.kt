package br.com.fenix.bilingualreader.view.ui.reader.manga

import android.content.Intent
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.custom.CustomTypes
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.service.repository.DataBase
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.utils.MangaTestUtil
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
class MangaReaderActivityTest {

    private val manga: Manga = MangaTestUtil.getManga(ApplicationProvider.getApplicationContext(), MangaTestUtil.mMangaPath)
    private var intent: Intent? = null

    init {
        assertTrue("Comic file informed not found, please verify declared 'filePath' in " + MangaReaderActivityTest::class.java.name, manga.file.exists())

        val dataBase = DataBase.getDataBase(ApplicationProvider.getApplicationContext()).getMangaDao()
        
        manga.bookMark = MangaTestUtil.mMangaPage
        manga.id = dataBase.save(manga)

        intent = Intent(ApplicationProvider.getApplicationContext(), MangaReaderActivity::class.java)

        val bundle = Bundle()
        bundle.putSerializable(GeneralConsts.KEYS.OBJECT.LIBRARY, manga.library)
        bundle.putSerializable(GeneralConsts.KEYS.OBJECT.MANGA, manga)
        bundle.putString(GeneralConsts.KEYS.MANGA.NAME, manga.title)
        bundle.putInt(GeneralConsts.KEYS.MANGA.PAGE_NUMBER, MangaTestUtil.mMangaPage)
        bundle.putInt(GeneralConsts.KEYS.MANGA.MARK, MangaTestUtil.mMangaPage)
        intent?.putExtras(bundle)
    }


    @get:Rule
    val activityScenarioRule = ActivityScenarioRule<MangaReaderActivity>(intent)

    private val awaitProcessSeconds = 2L

    @Test
    fun `0_test_manga_reader`() {
        val waiter = CountDownLatch(1)
        val scenario = activityScenarioRule.scenario

        scenario.onActivity {
            val fragment = it.supportFragmentManager.findFragmentById(R.id.root_frame_manga_reader)
            assertTrue(fragment is MangaReaderFragment)
        }

        waiter.await(10, TimeUnit.MINUTES)
    }

    @Test
    fun `1_test_manga_read`() {
        val waiter = CountDownLatch(1)
        val scenario = activityScenarioRule.scenario

        scenario.onActivity {
            val fragment = it.supportFragmentManager.findFragmentById(R.id.root_frame_manga_reader)
            assertTrue(fragment is MangaReaderFragment)
        }
        
        waiter.await(awaitProcessSeconds, TimeUnit.SECONDS)
        onView(withId(R.id.root_frame_manga_reader)).perform(click())

        waiter.await(awaitProcessSeconds, TimeUnit.SECONDS)
        onView(withId(R.id.reader_manga_btn_floating_popup)).perform(click())

        waiter.await(awaitProcessSeconds, TimeUnit.SECONDS)
        onView(withId(R.id.root_frame_manga_reader)).perform(click())

        waiter.await(awaitProcessSeconds, TimeUnit.SECONDS)
        onView(withId(R.id.reader_manga_btn_menu_page_linked)).perform(click())

        waiter.await(awaitProcessSeconds, TimeUnit.SECONDS)
        onView(withId(R.id.root_frame_manga_reader)).perform(click())

        waiter.await(awaitProcessSeconds, TimeUnit.SECONDS)
        onView(withId(R.id.reader_manga_btn_popup_color)).perform(click())

        waiter.await(awaitProcessSeconds, TimeUnit.SECONDS)
        onView(withId(R.id.root_frame_manga_reader)).perform(click())

        waiter.await(awaitProcessSeconds, TimeUnit.SECONDS)
        onView(withId(R.id.root_frame_manga_reader)).perform(click())

        waiter.await(awaitProcessSeconds, TimeUnit.SECONDS)
        onView(withId(R.id.reader_manga_btn_menu_ocr)).perform(click())

        waiter.await(awaitProcessSeconds, TimeUnit.SECONDS)
        onView(withId(R.id.root_frame_manga_reader)).perform(click())

        waiter.await(awaitProcessSeconds, TimeUnit.SECONDS)
        onView(withId(R.id.reader_manga_btn_screen_rotate)).perform(click())

        waiter.await(awaitProcessSeconds, TimeUnit.SECONDS)
        onView(withId(R.id.root_frame_manga_reader)).perform(click())

        waiter.await(awaitProcessSeconds, TimeUnit.SECONDS)
        onView(withId(R.id.reader_manga_nav_next_file)).perform(click())

        onView(withId(CustomTypes.AlertDialogButton.POSITIVE.resId)).perform(click())

        waiter.await(awaitProcessSeconds, TimeUnit.SECONDS)
        onView(withId(R.id.root_frame_manga_reader)).perform(click())

        waiter.await(awaitProcessSeconds, TimeUnit.SECONDS)
        onView(withId(R.id.reader_manga_nav_previous_file)).perform(click())

        onView(withId(CustomTypes.AlertDialogButton.POSITIVE.resId)).perform(click())

    }

}