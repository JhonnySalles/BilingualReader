package br.com.fenix.bilingualreader.view.ui.library.manga

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
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.action.ViewActions.swipeDown
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
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
import br.com.fenix.bilingualreader.view.components.TriStateCheckBox
import br.com.fenix.bilingualreader.view.ui.reader.manga.MangaReaderActivity
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
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
        
        // Limpa SharedPreferences para garantir estado inicial limpo (ordem alfabética)
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
        Intents.release()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val mockPath = File(context.cacheDir, "mock_mangas")
        if (mockPath.exists()) mockPath.deleteRecursively()
        db.close()
    }

    private fun launchFragment() {
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
    }

    @Test
    fun testMangaListIsDisplayed() {
        launchFragment()
        
        // Verifica se alguns dos itens sequenciais estão na lista
        waitForView(withText("manga 01"))
        onView(withText("manga 05")).check(matches(isDisplayed()))
        onView(withText("manga 10")).check(matches(isDisplayed()))
    }

    @Test
    fun testOpenTypePopup() {
        launchFragment()

        // Clica longo no botão de tipo para abrir o popup
        onView(withId(R.id.menu_manga_library_type)).perform(longClick())

        // Verifica se o popup de tipo está visível
        onView(withId(R.id.manga_library_popup_menu_library)).check(matches(isDisplayed()))
        
        // Verifica se as abas do TabLayout estão presentes
        onView(withText(R.string.popup_library_manga_tab_item_type)).check(matches(isDisplayed()))
    }

    @Test
    fun testPopupTypeInteractions() {
        launchFragment()
        onView(withId(R.id.menu_manga_library_type)).perform(longClick())
        
        // Clica em Grid Big
        onView(withId(R.id.popup_library_manga_type_grid_big)).perform(click())
        Thread.sleep(500)
        onView(withId(R.id.manga_library_recycler_view)).check(matches(hasDescendant(withId(R.id.manga_grid_text_title))))
        
        // Volta para Line
        onView(withId(R.id.popup_library_manga_type_line)).perform(click())
        Thread.sleep(500)
        onView(withId(R.id.manga_library_recycler_view)).check(matches(hasDescendant(withId(R.id.manga_line_text_title))))
    }

    @Test
    fun testOpenOrderPopup() {
        launchFragment()

        // Clica longo no botão de ordenação para abrir o popup
        onView(withId(R.id.menu_manga_library_list_order)).perform(longClick())
        
        // Clica na aba de ordenação
        onView(withText(R.string.popup_library_manga_tab_item_ordering)).perform(click())
        
        onView(withText(R.string.popup_library_manga_tab_item_ordering)).check(matches(isSelected()))
    }

    @Test
    fun testPopupOrderTriStateInteractions() {
        launchFragment()
        onView(withId(R.id.menu_manga_library_list_order)).perform(longClick())
        onView(withText(R.string.popup_library_manga_tab_item_ordering)).perform(click())
        
        // Default: Name Checked
        onView(withId(R.id.popup_library_order_manga_name)).check(matches(withTriStateState(TriStateCheckBox.STATE_CHECKED)))
        
        // Clica em Autor
        onView(withId(R.id.popup_library_order_manga_author)).perform(click())
        onView(withId(R.id.popup_library_order_manga_author)).check(matches(withTriStateState(TriStateCheckBox.STATE_CHECKED)))
        
        // Clica novamente em Autor (Indeterminate - Desc)
        onView(withId(R.id.popup_library_order_manga_author)).perform(click())
        onView(withId(R.id.popup_library_order_manga_author)).check(matches(withTriStateState(TriStateCheckBox.STATE_INDETERMINATE)))
    }

    @Test
    fun testPopupFilterInteractions() {
        launchFragment()
        onView(withId(R.id.menu_manga_library_type)).perform(longClick())
        onView(withText(R.string.popup_library_manga_tab_item_filter)).perform(click())
        
        // Filtra por Favorito (manga 05)
        onView(withId(R.id.popup_library_filter_favorite)).perform(click())
        Thread.sleep(500)
        onView(withId(R.id.manga_library_recycler_view)).check(matches(atPosition(0, hasDescendant(withText("manga 05")))))
    }

    @Test
    fun testSortingFunctionality() {
        launchFragment()

        // Verificação Inicial: Ordem alfabética (Padrão após clear prefs) -> manga 01 no topo
        onView(withId(R.id.manga_library_recycler_view))
            .check(matches(atPosition(0, hasDescendant(withText("manga 01")))))

        // 1. Clicar no botão de ordenação para mudar para 'Data' (Nome -> Data)
        onView(withId(R.id.menu_manga_library_list_order)).perform(click())
        waitForView(atPosition(0, hasDescendant(withText("manga 01"))))

        // 2. Clicar novamente para mudar para 'Favorito' (Data -> Favorito)
        onView(withId(R.id.menu_manga_library_list_order)).perform(click())

        // No ViewModel: Favoritos DESC (true primeiro), depois Nome ASC
        waitForView(atPosition(0, hasDescendant(withText("manga 05"))))
    }

    @Test
    fun testSearchFiltering() {
        launchFragment()
        onView(withId(R.id.menu_manga_library_search)).perform(click())
        onView(isAssignableFrom(AutoCompleteTextView::class.java)).perform(typeText("manga 08"))
        
        Thread.sleep(1000) // Debounce
        
        onView(withId(R.id.manga_library_recycler_view)).check(matches(atPosition(0, hasDescendant(withText("manga 08")))))
    }

    @Test
    fun testImportVocabularyMenu() {
        launchFragment()
        
        // Abre o menu overflow (necessário pois showAsAction="never")
        openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().targetContext)
        
        // Clica pelo texto do recurso
        onView(withText(R.string.menu_manga_vocabulary_import)).perform(click())
        
        // Verifica se o diálogo abriu
        onView(withText(R.string.vocabulary_import_title)).check(matches(isDisplayed()))
        
        // Verifica presença de uma das opções do array (Importação Completa)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val options = context.resources.getStringArray(R.array.import_vocabulary)
        onView(withText(options[1])).check(matches(isDisplayed()))
    }

    @Test
    fun testSwipeRefreshLayout() {
        launchFragment()
        onView(withId(R.id.manga_library_refresh)).perform(swipeDown())
        onView(withId(R.id.manga_library_recycler_view)).check(matches(isDisplayed()))
    }

    @Test
    fun testMangaItemClickNavigatesToReader() {
        launchFragment()
        
        intending(hasComponent(MangaReaderActivity::class.java.name))
            .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))
            
        onView(allOf(withId(R.id.manga_line_text_title), withText("manga 01"))).perform(click())
        
        intended(allOf(
            hasComponent(MangaReaderActivity::class.java.name),
            hasExtra(GeneralConsts.KEYS.MANGA.NAME, "manga 01")
        ))
    }

    @Test
    fun testMangaItemOptionsMenu() {
        launchFragment()
        
        // Clica especificamente no botão de configuração do primeiro item (index 0)
        onView(withId(R.id.manga_library_recycler_view))
            .perform(androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, clickChildViewWithId(R.id.manga_line_config)))
        
        // Aguarda o PopupMenu aparecer antes de realizar a verificação
        Thread.sleep(500)
        
        // PopupMenu exige inRoot(isPlatformPopup()) para ser localizado pelo Espresso
        onView(withText(R.string.menu_manga_config_detail))
            .inRoot(isPlatformPopup())
            .check(matches(isDisplayed()))
            
        onView(withText(R.string.menu_manga_config_delete))
            .inRoot(isPlatformPopup())
            .check(matches(isDisplayed()))
    }
}