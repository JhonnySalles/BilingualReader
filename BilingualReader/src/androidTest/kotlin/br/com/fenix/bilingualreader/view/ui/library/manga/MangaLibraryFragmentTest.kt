package br.com.fenix.bilingualreader.view.ui.library.manga

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.TestActivity
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.enums.Libraries
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.listener.MainListener
import br.com.fenix.bilingualreader.service.repository.DataBase
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

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

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
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
            
            val manga = Manga(libraryId, i.toLong(), file).apply {
                title = name
                author = "Author $i"
                favorite = (i == 5)
                dateCreate = java.time.LocalDateTime.now().minusDays((10 - i).toLong())
                excluded = false
            }
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
        
        Thread.sleep(2000)
        
        onView(withId(R.id.manga_library_recycler_view)).check(matches(isDisplayed()))
        
        // Verifica se alguns dos itens sequenciais estão na lista
        onView(withText("manga 01")).check(matches(isDisplayed()))
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
        
        // Aguarda carregamento
        Thread.sleep(2000)

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
        val scenario = ActivityScenario.launch(TestActivity::class.java)
        scenario.onActivity { activity ->
            val viewModel = ViewModelProvider(activity)[MangaLibraryViewModel::class.java]
            
            viewModel.setLibrary(createMockLibrary(activity))
            viewModel.list { }

            val fragment = MangaLibraryFragment()
            MangaLibraryFragment.setMainListener(fragment, activity)
            activity.setFragment(fragment)
        }
        
        // Aguarda carregamento
        Thread.sleep(2000)

        // Clica no botão de ordenação no menu
        onView(withId(R.id.menu_manga_library_type)).perform(click())
        onView(withText(R.string.popup_library_manga_tab_item_ordering)).perform(click())
        
        onView(withText(R.string.popup_library_manga_tab_item_ordering)).check(matches(isSelected()))
    }

    @Test
    fun testSortingFunctionality() {
        val scenario = ActivityScenario.launch(TestActivity::class.java)
        lateinit var viewModel: MangaLibraryViewModel
        scenario.onActivity { activity ->
            viewModel = ViewModelProvider(activity)[MangaLibraryViewModel::class.java]
            
            viewModel.setLibrary(createMockLibrary(activity))
            viewModel.list { }

            val fragment = MangaLibraryFragment()
            MangaLibraryFragment.setMainListener(fragment, activity)
            activity.setFragment(fragment)
        }
        
        Thread.sleep(2000)

        // Verificação Inicial: Ordem alfabética (Padrão) -> manga 01 no topo
        onView(withText("manga 01")).check(matches(isDisplayed()))

        // 1. Clicar no botão de ordenação para mudar para 'Data' (Nome -> Data)
        onView(withId(R.id.menu_manga_library_list_order)).perform(click())
        Thread.sleep(1000)
        
        // Verifica se a lista refletiu no ViewModel (manga 01 é o mais antigo na nossa população)
        // O ViewModel faz sortBy { it.dateCreate } (Ascendente)
        assert(viewModel.listMangas.value!![0].title == "manga 01")

        // 2. Clicar novamente para mudar para 'Favorito' (Data -> Favorito)
        onView(withId(R.id.menu_manga_library_list_order)).perform(click())
        Thread.sleep(1000)

        // No ViewModel: sortWith(compareByDescending<Manga> { it.favorite }.thenBy { it.name })
        // manga 05 é o único favorito, deve estar no topo
        onView(withText("manga 05")).check(matches(isDisplayed()))
        assert(viewModel.listMangas.value!![0].title == "manga 05")
    }
}