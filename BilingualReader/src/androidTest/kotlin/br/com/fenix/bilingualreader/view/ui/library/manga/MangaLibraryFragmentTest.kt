package br.com.fenix.bilingualreader.view.ui.library.manga

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
import androidx.test.platform.app.InstrumentationRegistry
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.TestActivity
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.enums.FileType
import br.com.fenix.bilingualreader.model.enums.Libraries
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.listener.MainListener
import br.com.fenix.bilingualreader.service.repository.DataBase
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.anyOf
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.util.Date

@RunWith(AndroidJUnit4::class)
class MangaLibraryFragmentTest {

    private lateinit var db: DataBase
    private val mainListener = object : MainListener {
        override fun showUpButton() {}
        override fun hideUpButton() {}
        override fun changeLibraryTitle(library: String) {}
        override fun clearLibraryTitle() {}
    }

    private fun createMockLibrary(context: Context, id: Long = GeneralConsts.KEYS.LIBRARY.DEFAULT_MANGA): Library {
        val mockPath = File(context.cacheDir, "mock_mangas")
        if (!mockPath.exists()) mockPath.mkdirs()
        
        return Library(
            id = id,
            title = "Manga Test Library",
            path = mockPath.absolutePath,
            language = Libraries.JAPANESE,
            type = Type.MANGA
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
        
        // Limpa SharedPreferences para garantir estado inicial limpo (ordem alfabética)
        val sharedPreferences = context.getSharedPreferences(GeneralConsts.KEYS.PREFERENCE_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().commit()

        db = Room.inMemoryDatabaseBuilder(context, DataBase::class.java)
            .allowMainThreadQueries()
            .build()
        
        DataBase.setTestingInstance(db)

        val libraryId = GeneralConsts.KEYS.LIBRARY.DEFAULT_MANGA
        val library = createMockLibrary(context, libraryId)
        db.getLibrariesDao().save(library)

        val mockPath = File(library.path)
        
        // Popula com 10 mangás físicos reais (copiados do manga.zip nos assets)
        for (i in 1..10) {
            val name = "manga %02d".format(i)
            val file = File(mockPath, "$name.cbz")
            
            // Copia o arquivo manga.zip dos assets do teste para o arquivo mockado
            InstrumentationRegistry.getInstrumentation().context.assets.open("manga.zip").use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            
            val manga = Manga(
                id = i.toLong(),
                title = name,
                path = file.path,
                folder = file.parent ?: "",
                name = file.name,
                fileSize = file.length(),
                fileType = FileType.UNKNOWN,
                pages = 1,
                chapters = intArrayOf(),
                chaptersPages = mapOf(),
                bookMark = 0,
                completed = false,
                favorite = (i == 5),
                hasSubtitle = false,
                author = "Author $i",
                series = "",
                genre = "",
                publisher = "",
                volume = "",
                release = null,
                fkLibrary = libraryId,
                excluded = false,
                dateCreate = LocalDateTime.now().minusDays((10 - i).toLong()),
                lastAccess = null,
                lastAlteration = null,
                fileAlteration = Date(file.lastModified()),
                lastVocabImport = null,
                lastVerify = null
            )
            db.getMangaDao().save(manga)
        }
    }

    @After
    fun closeDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val mockPath = File(context.cacheDir, "mock_mangas")
        if (mockPath.exists()) mockPath.deleteRecursively()
        db.close()
    }

    @Test
    fun testMangaListIsDisplayed() {
        val scenario = ActivityScenario.launch(TestActivity::class.java)
        scenario.onActivity { activity ->
            val viewModel = ViewModelProvider(activity)[MangaLibraryViewModel::class.java]
            
            viewModel.setLibrary(createMockLibrary(activity))
            viewModel.list { }

            val fragment = MangaLibraryFragment()
            MangaLibraryFragment.setMainListener(fragment, activity)
            activity.setFragment(fragment)
        }
        
        waitForView(withId(R.id.manga_library_recycler_view))
        
        scenario.onActivity { activity ->
            val recyclerView = activity.findViewById<RecyclerView>(R.id.manga_library_recycler_view)
            recyclerView.itemAnimator = null // Desativa animações para evitar AppNotIdle
        }
        
        // Aguarda o Skeleton sumir para garantir visibilidade da lista
        waitForSkeleton()
        
        // Verifica se alguns dos itens sequenciais estão na lista
        waitForView(withText("manga 01"))
        onView(withText("manga 05")).check(matches(isDisplayed()))
        onView(withText("manga 10")).check(matches(isDisplayed()))
    }

    @Test
    fun testOpenTypePopup() {
        val scenario = ActivityScenario.launch(TestActivity::class.java)
        scenario.onActivity { activity ->
            val viewModel = ViewModelProvider(activity)[MangaLibraryViewModel::class.java]
            
            viewModel.setLibrary(createMockLibrary(activity))
            viewModel.list { }

            val fragment = MangaLibraryFragment()
            MangaLibraryFragment.setMainListener(fragment, activity)
            activity.setFragment(fragment)
        }
        
        waitForView(withId(R.id.manga_library_recycler_view))
        
        scenario.onActivity { activity ->
            activity.findViewById<RecyclerView>(R.id.manga_library_recycler_view).itemAnimator = null
        }

        // Clica longo no botão de tipo para abrir o popup
        onView(withId(R.id.menu_manga_library_type)).perform(longClick())

        // Verifica se o popup de tipo está visível
        onView(withId(R.id.manga_library_popup_menu_library)).check(matches(isDisplayed()))
        
        // Verifica se as abas do TabLayout estão presentes
        onView(withText(R.string.popup_library_manga_tab_item_type)).check(matches(isDisplayed()))
    }

    @Test
    fun testOpenOrderPopup() {
        val scenario = ActivityScenario.launch(TestActivity::class.java)
        scenario.onActivity { activity ->
            val viewModel = ViewModelProvider(activity)[MangaLibraryViewModel::class.java]
            
            viewModel.setLibrary(createMockLibrary(activity))
            viewModel.list { }

            val fragment = MangaLibraryFragment()
            MangaLibraryFragment.setMainListener(fragment, activity)
            activity.setFragment(fragment)
        }
        
        waitForView(withId(R.id.manga_library_recycler_view))
        
        scenario.onActivity { activity ->
            activity.findViewById<RecyclerView>(R.id.manga_library_recycler_view).itemAnimator = null
        }

        // Clica longo no botão de ordenação para abrir o popup
        onView(withId(R.id.menu_manga_library_list_order)).perform(longClick())
        
        // Clica na aba de ordenação
        onView(withText(R.string.popup_library_manga_tab_item_ordering)).perform(click())
        
        onView(withText(R.string.popup_library_manga_tab_item_ordering)).check(matches(isSelected()))
    }

    @Test
    fun testSortingFunctionality() {
        val scenario = ActivityScenario.launch(TestActivity::class.java)
        scenario.onActivity { activity ->
            val viewModel = ViewModelProvider(activity)[MangaLibraryViewModel::class.java]
            
            viewModel.setLibrary(createMockLibrary(activity))
            viewModel.list { }

            val fragment = MangaLibraryFragment()
            MangaLibraryFragment.setMainListener(fragment, activity)
            activity.setFragment(fragment)
        }
        
        waitForView(withId(R.id.manga_library_recycler_view))
        
        scenario.onActivity { activity ->
            activity.findViewById<RecyclerView>(R.id.manga_library_recycler_view).itemAnimator = null
        }

        waitForSkeleton()

        // Verificação Inicial: Ordem alfabética (Padrão após clear prefs) -> manga 01 no topo
        onView(withId(R.id.manga_library_recycler_view))
            .check(matches(atPosition(0, hasDescendant(withText("manga 01")))))

        // 1. Clicar no botão de ordenação para mudar para 'Data' (Nome -> Data)
        // O click muda o ciclo de ordenação no fragmento e salva no SharedPreferences
        onView(withId(R.id.menu_manga_library_list_order)).perform(click())
        
        // Aguarda a atualização da lista. Manga 01 é o mais antigo (minusDays(9)), então continua em 0
        waitForView(atPosition(0, hasDescendant(withText("manga 01"))))

        // 2. Clicar novamente para mudar para 'Favorito' (Data -> Favorito)
        onView(withId(R.id.menu_manga_library_list_order)).perform(click())

        // No ViewModel: Favoritos DESC (true primeiro), depois Nome ASC
        // Manga 05 é o único favorito na nossa população mockada
        waitForView(atPosition(0, hasDescendant(withText("manga 05"))))
    }
}