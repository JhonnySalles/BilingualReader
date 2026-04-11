package br.com.fenix.bilingualreader.view.ui.library.book

import android.content.Context
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.Visibility
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

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        // Limpa SharedPreferences para garantir estado inicial limpo
        val sharedPreferences = context.getSharedPreferences(GeneralConsts.KEYS.PREFERENCE_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().commit()

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
                genre = "",
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
        val context = ApplicationProvider.getApplicationContext<Context>()
        val mockPath = File(context.cacheDir, "mock_books")
        if (mockPath.exists()) mockPath.deleteRecursively()
        db.close()
    }

    @Test
    fun testBookListIsDisplayed() {
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
        
        waitForView(allOf(withId(R.id.book_line_title), withText("book 01")))
        onView(allOf(withId(R.id.book_line_title), withText("book 05"))).check(matches(isDisplayed()))
        onView(allOf(withId(R.id.book_line_title), withText("book 10"))).check(matches(isDisplayed()))
    }

    @Test
    fun testOpenTypePopup() {
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

        // Abre o popup com clique longo
        onView(withId(R.id.menu_book_library_type)).perform(longClick())

        onView(withId(R.id.book_library_popup_menu_library)).check(matches(isDisplayed()))
        onView(withText(R.string.popup_library_book_tab_item_type)).check(matches(isDisplayed()))
    }

    @Test
    fun testOpenOrderPopup() {
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

        // Abre o popup com clique longo
        onView(withId(R.id.menu_book_library_type)).perform(longClick())
        onView(withText(R.string.popup_library_book_tab_item_ordering)).perform(click())
        
        onView(withText(R.string.popup_library_book_tab_item_ordering)).check(matches(isSelected()))
    }

    @Test
    fun testSortingFunctionality() {
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

        // Verificação Inicial: Ordem alfabética (Padrão) -> book 01 no topo
        onView(withId(R.id.book_library_recycler_view))
            .check(matches(atPosition(0, hasDescendant(allOf(withId(R.id.book_line_title), withText("book 01"))))))

        // 1. Clicar no botão de ordenação para mudar para 'Data' (Nome -> Data)
        onView(withId(R.id.menu_book_library_list_order)).perform(click())
        
        // Aguarda a atualização da lista. book 01 é o mais antigo, continua no topo
        waitForView(atPosition(0, hasDescendant(allOf(withId(R.id.book_line_title), withText("book 01")))))

        // 2. Clicar novamente para mudar para 'Favorito' (Data -> Favorito)
        onView(withId(R.id.menu_book_library_list_order)).perform(click())

        // No ViewModel: Favoritos primeiro, depois Nome. book 05 é favorito.
        waitForView(atPosition(0, hasDescendant(allOf(withId(R.id.book_line_title), withText("book 05")))))
    }
}
