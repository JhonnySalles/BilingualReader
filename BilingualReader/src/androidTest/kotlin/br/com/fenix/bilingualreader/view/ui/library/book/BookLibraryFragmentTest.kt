package br.com.fenix.bilingualreader.view.ui.library.book

import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.view.View
import android.widget.AutoCompleteTextView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.action.ViewActions.swipeDown
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isSelected
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.TestActivity
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.enums.FileType
import br.com.fenix.bilingualreader.model.enums.Languages
import br.com.fenix.bilingualreader.model.enums.Libraries
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.listener.MainListener
import br.com.fenix.bilingualreader.service.repository.DataBase
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.view.components.TriStateCheckBox
import br.com.fenix.bilingualreader.view.ui.reader.book.BookReaderActivity
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.anyOf
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDateTime
import java.util.Date

@RunWith(AndroidJUnit4::class)
class BookLibraryFragmentTest {

    private lateinit var db: DataBase
    private val mainListener = object : MainListener {
        override fun showUpButton() {}
        override fun hideUpButton() {}
        override fun changeLibraryTitle(library: String) {}
        override fun clearLibraryTitle() {}
    }

    private fun createMockLibrary(context: Context, id: Long = GeneralConsts.KEYS.LIBRARY.DEFAULT_BOOK): Library {
        val mockPath = File(context.cacheDir, "mock_books")
        if (!mockPath.exists()) mockPath.mkdirs()
        
        return Library(
            id = id,
            title = "Book Test Library",
            path = mockPath.absolutePath,
            language = Libraries.ENGLISH,
            type = Type.BOOK
        )
    }

    private fun waitForView(matcher: Matcher<View>, timeout: Long = 5000): ViewInteraction {
        val startTime = System.currentTimeMillis()
        val endTime = startTime + timeout

        while (System.currentTimeMillis() < endTime) {
            try {
                val interaction = onView(matcher)
                interaction.check(matches(isDisplayed()))
                return interaction
            } catch (e: Throwable) {
                Thread.sleep(100)
            }
        }
        return onView(matcher).check(matches(isDisplayed()))
    }

    private fun clickChildViewWithId(id: Int): androidx.test.espresso.ViewAction {
        return object : androidx.test.espresso.ViewAction {
            override fun getConstraints(): org.hamcrest.Matcher<View> = allOf(isAssignableFrom(View::class.java), isDisplayed())
            override fun getDescription(): String = "Click on a child view with specified id."
            override fun perform(uiController: androidx.test.espresso.UiController, view: View) {
                val v = view.findViewById<View>(id)
                v.performClick()
            }
        }
    }

    private fun waitForSkeleton() {
        val startTime = System.currentTimeMillis()
        val timeout = 5000L
        while (System.currentTimeMillis() < startTime + timeout) {
            try {
                onView(withId(R.id.skeleton_layout)).check(matches(anyOf(withEffectiveVisibility(Visibility.GONE), withEffectiveVisibility(Visibility.INVISIBLE))))
                return
            } catch (e: Throwable) {
                Thread.sleep(100)
            }
        }
    }

    private fun atPosition(position: Int, itemMatcher: Matcher<View>): Matcher<View> {
        return object : BoundedMatcher<View, RecyclerView>(RecyclerView::class.java) {
            override fun describeTo(description: Description) {
                description.appendText("has item at position $position: ")
                itemMatcher.describeTo(description)
            }
            override fun matchesSafely(view: RecyclerView): Boolean {
                val viewHolder = view.findViewHolderForAdapterPosition(position)
                    ?: return false
                return itemMatcher.matches(viewHolder.itemView)
            }
        }
    }

    private fun withTriStateState(state: Int): Matcher<View> {
        return object : BoundedMatcher<View, TriStateCheckBox>(TriStateCheckBox::class.java) {
            override fun describeTo(description: Description) {
                description.appendText("with TriStateCheckBox state: $state")
            }
            override fun matchesSafely(item: TriStateCheckBox): Boolean {
                return item.state == state
            }
        }
    }

    @Before
    fun createDb() {
        Intents.init()
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        // Limpa SharedPreferences para garantir estado inicial limpo
        val sharedPreferences = context.getSharedPreferences(GeneralConsts.KEYS.PREFERENCE_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().commit()

        // Força o tipo de biblioteca como LINE para garantir preditividade nos testes de ID
        sharedPreferences.edit()
            .putString(GeneralConsts.KEYS.LIBRARY.MANGA_LIBRARY_TYPE, br.com.fenix.bilingualreader.model.enums.LibraryMangaType.LINE.toString())
            .putString(GeneralConsts.KEYS.LIBRARY.BOOK_LIBRARY_TYPE, br.com.fenix.bilingualreader.model.enums.LibraryBookType.LINE.toString())
            .commit()

        db = Room.inMemoryDatabaseBuilder(context, DataBase::class.java)
            .allowMainThreadQueries()
            .build()
        
        DataBase.setTestingInstance(db)

        val libraryId = GeneralConsts.KEYS.LIBRARY.DEFAULT_BOOK
        val library = createMockLibrary(context, libraryId)
        db.getLibrariesDao().save(library)

        val mockPath = File(library.path)
        
        // Popula com 10 livros
        for (i in 1..10) {
            val name = "book %02d".format(i)
            val file = File(mockPath, "$name.epub")
            if (!file.exists()) file.createNewFile()
            
            val book = Book(
                id = i.toLong(),
                title = name,
                author = "Author $i",
                password = "",
                annotation = "",
                release = null,
                genre = "Genre $i",
                publisher = "",
                series = "",
                isbn = "",
                pages = 1,
                volume = "",
                chapter = 0,
                chapterDescription = "",
                bookMark = 0,
                completed = false,
                language = Languages.ENGLISH,
                path = file.path,
                folder = file.parent ?: "",
                name = file.name,
                fileType = FileType.EPUB,
                fileSize = 0L,
                favorite = (i == 5),
                fkLibrary = libraryId,
                tags = mutableListOf(),
                excluded = false,
                dateCreate = LocalDateTime.now().minusDays((10 - i).toLong()),
                lastAccess = null,
                lastAlteration = null,
                fileAlteration = Date(file.lastModified()),
                lastVocabImport = null,
                lastVerify = null
            )
            db.getBookDao().save(book)
        }
    }

    @After
    fun closeDb() {
        Intents.release()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val mockPath = File(context.cacheDir, "mock_books")
        if (mockPath.exists()) mockPath.deleteRecursively()
        db.close()
    }

    private fun launchFragment() {
        val scenario = ActivityScenario.launch(TestActivity::class.java)
        scenario.onActivity { activity ->
            val viewModel = ViewModelProvider(activity)[BookLibraryViewModel::class.java]
            viewModel.setLibrary(createMockLibrary(activity))
            viewModel.list { }

            val fragment = BookLibraryFragment()
            BookLibraryFragment.setMainListener(fragment, activity)
            activity.setFragment(fragment)
        }
        waitForView(withId(R.id.book_library_recycler_view))
        scenario.onActivity { activity ->
            activity.findViewById<RecyclerView>(R.id.book_library_recycler_view).itemAnimator = null
        }
        waitForSkeleton()
    }

    @Test
    fun testBookListIsDisplayed() {
        launchFragment()
        waitForView(allOf(withId(R.id.book_line_title), withText("book 01")))
        onView(allOf(withId(R.id.book_line_title), withText("book 05"))).check(matches(isDisplayed()))
        onView(allOf(withId(R.id.book_line_title), withText("book 10"))).check(matches(isDisplayed()))
    }

    @Test
    fun testOpenTypePopup() {
        launchFragment()
        onView(withId(R.id.menu_book_library_type)).perform(longClick())
        onView(withId(R.id.book_library_popup_menu_library)).check(matches(isDisplayed()))
        onView(withText(R.string.popup_library_book_tab_item_type)).check(matches(isDisplayed()))
    }

    @Test
    fun testPopupTypeInteractions() {
        launchFragment()
        onView(withId(R.id.menu_book_library_type)).perform(longClick())
        
        // Muda para Grid Big
        onView(withId(R.id.popup_library_book_type_grid_big)).perform(click())
        Thread.sleep(500) // Aguarda troca de layout
        onView(withId(R.id.book_library_recycler_view)).check(matches(hasDescendant(withId(R.id.book_grid_title))))
        
        // Volta para Line
        onView(withId(R.id.popup_library_book_type_line)).perform(click())
        Thread.sleep(500)
        onView(withId(R.id.book_library_recycler_view)).check(matches(hasDescendant(withId(R.id.book_line_title))))
    }

    @Test
    fun testOpenOrderPopup() {
        launchFragment()
        onView(withId(R.id.menu_book_library_type)).perform(longClick())
        onView(withText(R.string.popup_library_book_tab_item_ordering)).perform(click())
        onView(withText(R.string.popup_library_book_tab_item_ordering)).check(matches(isSelected()))
    }

    @Test
    fun testPopupOrderTriStateInteractions() {
        launchFragment()
        onView(withId(R.id.menu_book_library_list_order)).perform(longClick())
        
        // Default: Name Checked
        onView(withId(R.id.popup_library_order_book_name)).check(matches(withTriStateState(TriStateCheckBox.STATE_CHECKED)))
        
        // Clica em Autor
        onView(withId(R.id.popup_library_order_book_author)).perform(click())
        onView(withId(R.id.popup_library_order_book_author)).check(matches(withTriStateState(TriStateCheckBox.STATE_CHECKED)))
        onView(withId(R.id.popup_library_order_book_name)).check(matches(withTriStateState(TriStateCheckBox.STATE_UNCHECKED)))
        
        // Clica em Autor de novo (Indeterminate - Descendente)
        onView(withId(R.id.popup_library_order_book_author)).perform(click())
        onView(withId(R.id.popup_library_order_book_author)).check(matches(withTriStateState(TriStateCheckBox.STATE_INDETERMINATE)))
    }

    @Test
    fun testPopupFilterInteractions() {
        launchFragment()
        onView(withId(R.id.menu_book_library_type)).perform(longClick())
        onView(withText(R.string.popup_library_book_tab_item_filter)).perform(click())
        
        // Filtra por Favoritos (book 05 é favorito)
        onView(withId(R.id.popup_library_filter_favorite)).perform(click())
        Thread.sleep(500)
        onView(withId(R.id.book_library_recycler_view)).check(matches(atPosition(0, hasDescendant(withText("book 05")))))
    }

    @Test
    fun testSortingFunctionality() {
        launchFragment()

        // Verificação Inicial: Ordem alfabética (Padrão) -> book 01 no topo
        onView(withId(R.id.book_library_recycler_view))
            .check(matches(atPosition(0, hasDescendant(allOf(withId(R.id.book_line_title), withText("book 01"))))))

        // 1. Clicar no botão de ordenação para mudar para 'Data' (Nome -> Data)
        onView(withId(R.id.menu_book_library_list_order)).perform(click())
        waitForView(atPosition(0, hasDescendant(allOf(withId(R.id.book_line_title), withText("book 01")))))

        // 2. Clicar novamente para mudar para 'Favorito' (Data -> Favorito)
        onView(withId(R.id.menu_book_library_list_order)).perform(click())

        // No ViewModel: Favoritos primeiro, depois Nome. book 05 é favorito.
        waitForView(atPosition(0, hasDescendant(allOf(withId(R.id.book_line_title), withText("book 05")))))
    }

    @Test
    fun testSearchFiltering() {
        launchFragment()
        
        onView(withId(R.id.menu_book_library_search)).perform(click())
        onView(isAssignableFrom(AutoCompleteTextView::class.java)).perform(typeText("book 07"))
        
        Thread.sleep(1000) // Debounce
        
        onView(withId(R.id.book_library_recycler_view)).check(matches(atPosition(0, hasDescendant(withText("book 07")))))
        // book 01 não deve aparecer na hierarquia após filtragem física no adaptador
        onView(withText("book 01")).check(androidx.test.espresso.assertion.ViewAssertions.doesNotExist())
    }

    @Test
    fun testSearchWithTags() {
        launchFragment()
        
        onView(withId(R.id.menu_book_library_search)).perform(click())
        // Filtro por autor: @Author:Author 3
        onView(isAssignableFrom(AutoCompleteTextView::class.java)).perform(typeText("@Author:\"Author 3\""))
        
        // Aguarda a sincronização com o debounce da busca, especificando o ID para evitar ambiguidade com o nome do arquivo
        waitForView(allOf(withId(R.id.book_line_title), withText("book 03")))
        onView(allOf(withId(R.id.book_line_title), withText("book 03"))).check(matches(isDisplayed()))
    }

    @Test
    fun testSwipeRefreshLayout() {
        launchFragment()
        
        onView(withId(R.id.book_library_refresh)).perform(swipeDown())
        // O refreshing deve sumir rápido ou ficar visível enquanto o scanner roda
        // Difícil testar o estado exato sem IdlingResource, mas verificamos se a view ainda é funcional
        onView(withId(R.id.book_library_recycler_view)).check(matches(isDisplayed()))
    }

    @Test
    fun testBookItemClickNavigatesToReader() {
        launchFragment()

        intending(hasComponent(BookReaderActivity::class.java.name))
            .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))

        onView(allOf(withId(R.id.book_line_title), withText("book 01"))).perform(click())

        intended(allOf(
            hasComponent(BookReaderActivity::class.java.name),
            hasExtra(GeneralConsts.KEYS.BOOK.NAME, "book 01")
        ))
    }

    @Test
    fun testBookItemFavoriteToggle() {
        launchFragment()
        
        // Clica especificamente no ícone de favorito do primeiro item (book 01)
        onView(withId(R.id.book_library_recycler_view))
            .perform(androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, clickChildViewWithId(R.id.book_line_favorite)))
        
        // Verifica se o estado mudou mudando a ordenação para favorito
        onView(withId(R.id.menu_book_library_list_order)).perform(click()) // Muda para Data
        onView(withId(R.id.menu_book_library_list_order)).perform(click()) // Muda para Favorito
        
        // Agora book 01 e book 05 devem estar no topo (ordem alfabética entre favoritos)
        waitForView(atPosition(0, hasDescendant(allOf(withId(R.id.book_line_title), withText("book 01")))))
    }

    @Test
    fun testBookItemOptionsMenu() {
        launchFragment()
        
        // Clica especificamente no botão de configuração do primeiro item
        onView(withId(R.id.book_library_recycler_view))
            .perform(androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, clickChildViewWithId(R.id.book_line_config)))
        
        // Verifica se o popup menu abriu
        onView(withText(R.string.menu_book_config_detail)).check(matches(isDisplayed()))
        onView(withText(R.string.menu_book_config_send)).check(matches(isDisplayed()))
    }
}
