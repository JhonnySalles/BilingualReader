package br.com.fenix.bilingualreader.view.ui.menu

import android.content.Intent
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.BookAnnotation
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.repository.BookAnnotationRepository
import br.com.fenix.bilingualreader.service.repository.DataBase
import br.com.fenix.bilingualreader.service.repository.LibraryRepository
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.utils.LibraryTestUtil
import junit.framework.TestCase
import org.junit.AfterClass
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4::class)
class MenuActivityTest {

    private var intentSelectManga: Intent? = null
    private var intentLibrary: Intent? = null

    companion object {
        private val libraries = arrayListOf<Library>()

        @JvmStatic
        @AfterClass
        fun clear(): Unit {
            val repository = DataBase.getDataBase(ApplicationProvider.getApplicationContext()).getLibrariesDao()
            for (lib in libraries)
                repository.delete(lib)
        }
    }

    init {
        val repository = DataBase.getDataBase(ApplicationProvider.getApplicationContext()).getLibrariesDao()
        libraries.addAll(LibraryTestUtil.getArrayLibraries())
        for (lib in libraries) {
            repository.find(lib.id!!)?.let { repository.delete(it) }
            repository.save(lib)
        }
    }

    init {
        intentSelectManga = Intent(ApplicationProvider.getApplicationContext(), MenuActivity::class.java)

        val bundleSelectManga = Bundle()
        bundleSelectManga.putInt(GeneralConsts.KEYS.FRAGMENT.ID, R.id.frame_select_manga)
        intentSelectManga?.putExtras(bundleSelectManga)

        intentLibrary = Intent(ApplicationProvider.getApplicationContext(), MenuActivity::class.java)

        val bundleLibrary = Bundle()
        bundleLibrary.putInt(GeneralConsts.KEYS.FRAGMENT.ID, R.id.frame_config_libraries)
        bundleLibrary.putString(GeneralConsts.KEYS.LIBRARY.LIBRARY_TYPE, Type.MANGA.toString())
        intentLibrary?.putExtras(bundleLibrary)

    }


    @get:Rule
    val activitySelectMangaRule = ActivityScenarioRule<MenuActivity>(intentSelectManga)

    @get:Rule
    val activityLibraryRule = ActivityScenarioRule<MenuActivity>(intentLibrary)

    private val awaitProcessSeconds = 2L

    @Test
    fun `1_test_select_manga`() {
        val waiter = CountDownLatch(1)
        val scenario = activitySelectMangaRule.scenario

        scenario.onActivity {
            val fragment = it.supportFragmentManager.findFragmentById(R.id.root_frame_menu)
            TestCase.assertTrue(fragment is SelectMangaFragment)
        }

        waiter.await()
    }

    @Test
    fun `2_test_library`() {
        val waiter = CountDownLatch(1)


        waiter.await(10, TimeUnit.MINUTES)
    }
}