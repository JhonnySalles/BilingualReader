package br.com.fenix.bilingualreader.view.ui.pages_link

import android.content.Context
import android.content.Intent
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.enums.Libraries
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.repository.DataBase
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class PagesLinkFragmentTest {

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
        val mockPath = File(context.cacheDir, "mock_mangas_link")
        if (!mockPath.exists()) mockPath.mkdirs()
        
        mockLib = Library(
            id = 1L,
            title = "Test Link Library",
            path = mockPath.absolutePath,
            language = Libraries.ENGLISH,
            type = Type.MANGA
        )
        db.getLibrariesDao().save(mockLib)

        // Setup Manga
        val mangaFile = File(mockPath, "manga_link_test.zip")
        if (!mangaFile.exists()) mangaFile.createNewFile()

        mockManga = Manga(mockLib.id, 200L, mangaFile).apply {
            title = "Link Test Manga"
            pages = 10
            bookMark = 1
        }
        db.getMangaDao().save(mockManga)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun getStartIntent(): Intent {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return Intent(context, PagesLinkActivity::class.java).apply {
            putExtra(GeneralConsts.KEYS.OBJECT.MANGA, mockManga)
            putExtra(GeneralConsts.KEYS.MANGA.PAGE_NUMBER, 0)
        }
    }

    @Test
    fun testUIVisibility() {
        ActivityScenario.launch<PagesLinkActivity>(getStartIntent())
        
        // Aguarda transição e renderização
        Thread.sleep(2000)

        // Verifica o nome do mangá no topo
        onView(withId(R.id.pages_link_name_manga)).check(matches(withText(mockManga.name)))
        
        // Verifica se os recyclers principais estão visíveis
        onView(withId(R.id.pages_link_pages_linked_recycler)).check(matches(isDisplayed()))
        onView(withId(R.id.pages_link_pages_not_linked_recycler)).check(matches(isDisplayed()))
    }

    @Test
    fun testActionButtons() {
        ActivityScenario.launch<PagesLinkActivity>(getStartIntent())
        
        Thread.sleep(2000)

        // Testa se os botões de processamento existem e são clicáveis (sem causar crash)
        onView(withId(R.id.pages_link_auto_process_button)).perform(click())
        onView(withId(R.id.pages_link_reorder_button)).perform(click())
        onView(withId(R.id.pages_link_single_page_button)).perform(click())
    }

    @Test
    fun testDeleteLinkDialog() {
        ActivityScenario.launch<PagesLinkActivity>(getStartIntent())
        
        Thread.sleep(2000)

        // Clica no botão de deletar vínculo
        onView(withId(R.id.file_link_delete_button)).perform(click())
        
        // Verifica se o diálogo de confirmação apareceu
        onView(withText(R.string.page_link_delete_description)).check(matches(isDisplayed()))
        
        // Cancela
        onView(withText(R.string.action_negative)).perform(click())
        
        // Verifica se fechou
        Thread.sleep(500)
        onView(withText(R.string.page_link_delete_description)).check(doesNotExist())
    }
}
