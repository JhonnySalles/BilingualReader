package br.com.fenix.bilingualreader.view.ui.library.manga

import android.content.Context
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.enums.Libraries
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.listener.MainListener
import br.com.fenix.bilingualreader.service.repository.DataBase
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class MangaLibraryFragmentTest {

    private lateinit var db: DataBase
    private val mainListener = object : MainListener {
        override fun showUpButton() {}
        override fun hideUpButton() {}
        override fun changeLibraryTitle(library: String) {}
        override fun clearLibraryTitle() {}
    }

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, DataBase::class.java)
            .allowMainThreadQueries()
            .build()
        
        // Injeta o banco de dados em memória no Singleton
        DataBase.setTestingInstance(db)

        // Insere dados iniciais
        // O ViewModel usa GeneralConsts.KEYS.LIBRARY.DEFAULT_MANGA (-1L) como padrão
        val library = Library(
            id = -1L,
            title = "Manga Test Library",
            path = "/mock/path",
            language = Libraries.JAPANESE,
            type = Type.MANGA
        )
        db.getLibrariesDao().save(library)

        val manga1 = Manga(-1L, 1L, File("/mock/path/manga1.cbz")).apply {
            title = "Manga Alpha"
            author = "Author A"
            excluded = false
        }
        val manga2 = Manga(-1L, 2L, File("/mock/path/manga2.cbz")).apply {
            title = "Manga Beta"
            author = "Author B"
            excluded = false
        }
        
        db.getMangaDao().save(manga1)
        db.getMangaDao().save(manga2)
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testMangaListIsDisplayed() {
        val scenario = launchFragmentInContainer<MangaLibraryFragment>(
            themeResId = R.style.Theme_MangaReader,
            initialState = Lifecycle.State.CREATED
        )
        scenario.onFragment { MangaLibraryFragment.setMainListener(it, mainListener) }
        scenario.moveToState(Lifecycle.State.RESUMED)
        
        // Verifica se o RecyclerView está visível
        onView(withId(R.id.manga_library_recycler_view)).check(matches(isDisplayed()))
        
        // Verifica se os itens inseridos estão na lista (pelo menos um deles)
        onView(withText("Manga Alpha")).check(matches(isDisplayed()))
        onView(withText("Manga Beta")).check(matches(isDisplayed()))
    }

    @Test
    fun testOpenTypePopup() {
        val scenario = launchFragmentInContainer<MangaLibraryFragment>(
            themeResId = R.style.Theme_MangaReader,
            initialState = Lifecycle.State.CREATED
        )
        scenario.onFragment { MangaLibraryFragment.setMainListener(it, mainListener) }
        scenario.moveToState(Lifecycle.State.RESUMED)

        // Clica no botão de tipo de grid no menu
        onView(withId(R.id.menu_manga_library_type)).perform(click())

        // Verifica se o bottom sheet ou o conteúdo do popup de tipo está visível
        // O fragment utiliza um BottomSheetBehavior no R.id.manga_library_popup_menu_library
        onView(withId(R.id.manga_library_popup_menu_library)).check(matches(isDisplayed()))
        
        // Verifica se as abas do TabLayout estão presentes
        onView(withText(R.string.popup_library_manga_tab_item_type)).check(matches(isDisplayed()))
    }

    @Test
    fun testOpenOrderPopup() {
        val scenario = launchFragmentInContainer<MangaLibraryFragment>(
            themeResId = R.style.Theme_MangaReader,
            initialState = Lifecycle.State.CREATED
        )
        scenario.onFragment { MangaLibraryFragment.setMainListener(it, mainListener) }
        scenario.moveToState(Lifecycle.State.RESUMED)

        // Clica no botão de ordenação no menu
        onView(withId(R.id.menu_manga_library_type)).perform(click())
        onView(withText(R.string.popup_library_manga_tab_item_ordering)).perform(click())
        
        onView(withText(R.string.popup_library_manga_tab_item_ordering)).check(matches(isSelected()))
    }
}
