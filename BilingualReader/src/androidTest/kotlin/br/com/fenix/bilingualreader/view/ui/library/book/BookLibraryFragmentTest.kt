package br.com.fenix.bilingualreader.view.ui.library.book

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
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.enums.Libraries
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.repository.DataBase
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class BookLibraryFragmentTest {

    private lateinit var db: DataBase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, DataBase::class.java)
            .allowMainThreadQueries()
            .build()
        
        // Injeta o banco de dados em memória no Singleton
        DataBase.setTestingInstance(db)

        // Insere dados iniciais
        // O ViewModel usa GeneralConsts.KEYS.LIBRARY.DEFAULT_BOOK (-2L) como padrão
        val library = Library(
            id = -2L,
            title = "Book Test Library",
            path = "/mock/path/books",
            language = Libraries.ENGLISH,
            type = Type.BOOK
        )
        db.getLibrariesDao().save(library)

        val book1 = Book(-2L, 1L, File("/mock/path/books/book1.epub")).apply {
            title = "Book Alpha"
            author = "Author A"
            excluded = false
        }
        val book2 = Book(-2L, 2L, File("/mock/path/books/book2.pdf")).apply {
            title = "Book Beta"
            author = "Author B"
            excluded = false
        }
        
        db.getBookDao().save(book1)
        db.getBookDao().save(book2)
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testBookListIsDisplayed() {
        val scenario = launchFragmentInContainer<BookLibraryFragment>(themeResId = R.style.Theme_MangaReader)
        scenario.moveToState(Lifecycle.State.RESUMED)
        
        // Verifica se o RecyclerView está visível
        onView(withId(R.id.book_library_recycler_view)).check(matches(isDisplayed()))
        
        // Verifica se os itens inseridos estão na lista (pelo menos um deles)
        onView(withText("Book Alpha")).check(matches(isDisplayed()))
        onView(withText("Book Beta")).check(matches(isDisplayed()))
    }

    @Test
    fun testOpenTypePopup() {
        val scenario = launchFragmentInContainer<BookLibraryFragment>(themeResId = R.style.Theme_MangaReader)
        scenario.moveToState(Lifecycle.State.RESUMED)

        // Clica no botão de tipo de grid no menu
        onView(withId(R.id.menu_book_library_type)).perform(click())

        // Verifica se o bottom sheet ou o conteúdo do popup de tipo está visível
        onView(withId(R.id.book_library_popup_menu_library)).check(matches(isDisplayed()))
        
        // Verifica se as abas do TabLayout estão presentes
        onView(withText(R.string.popup_library_book_tab_item_type)).check(matches(isDisplayed()))
    }

    @Test
    fun testOpenOrderPopup() {
        val scenario = launchFragmentInContainer<BookLibraryFragment>(themeResId = R.style.Theme_MangaReader)
        scenario.moveToState(Lifecycle.State.RESUMED)

        // Abre o menu para testar navegação de abas
        onView(withId(R.id.menu_book_library_type)).perform(click())
        onView(withText(R.string.popup_library_book_tab_item_ordering)).perform(click())
        
        onView(withText(R.string.popup_library_book_tab_item_ordering)).check(matches(isSelected()))
    }
}
