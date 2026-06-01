package br.com.fenix.bilingualreader

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.contrib.NavigationViewActions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.enums.FileType
import br.com.fenix.bilingualreader.model.enums.Libraries
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.repository.DataBase
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.anyOf
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import androidx.room.Room
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.util.Date

@RunWith(AndroidJUnit4::class)
class MangaLibraryTest {

    private lateinit var db: DataBase
    private val waitTime = 3000L

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // 1. Limpar SharedPreferences para garantir estado inicial conhecido
        val sharedPreferences = context.getSharedPreferences(GeneralConsts.KEYS.PREFERENCE_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().commit()

        // Força o tipo de biblioteca como LINE para garantir preditividade nos testes
        sharedPreferences.edit()
            .putString(GeneralConsts.KEYS.LIBRARY.MANGA_LIBRARY_TYPE, br.com.fenix.bilingualreader.model.enums.LibraryMangaType.LINE.toString())
            .putLong(GeneralConsts.KEYS.LIBRARY.LAST_LIBRARY, GeneralConsts.KEYS.LIBRARY.DEFAULT_MANGA)
            .commit()

        // 2. Inicializar Banco de Dados em Memória
        db = Room.inMemoryDatabaseBuilder(context, DataBase::class.java)
            .allowMainThreadQueries()
            .build()
        DataBase.setTestingInstance(db)

        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("appops set ${context.packageName} MANAGE_EXTERNAL_STORAGE allow")

        // 3. Criar Biblioteca Mock
        val libraryId = GeneralConsts.KEYS.LIBRARY.DEFAULT_MANGA
        val mockPath = File(context.cacheDir, "mock_mangas")
        if (!mockPath.exists()) mockPath.mkdirs()

        val library = Library(
            id = libraryId,
            title = "Manga Test Library",
            path = mockPath.absolutePath,
            language = Libraries.JAPANESE,
            type = Type.MANGA
        )
        db.getLibrariesDao().save(library)

        // 4. Popular com Mangás (Copiando do asset manga.zip)
        for (i in 1..5) {
            val name = "manga %02d".format(i)
            val file = File(mockPath, "$name.cbz")

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
                pages = 10,
                chapters = intArrayOf(1),
                chaptersPages = mapOf(0 to "0"),
                bookMark = 0,
                completed = false,
                favorite = (i == 3), // Um favorito para o teste
                author = if (i == 3) "Fenix" else "Author $i",
                fkLibrary = libraryId,
                dateCreate = LocalDateTime.now().minusDays(i.toLong()),
                fileAlteration = Date(file.lastModified()),
                hasSubtitle = false,
                series = "",
                genre = "",
                publisher = "",
                volume = "",
                release = null,
                excluded = false,
                lastAccess = null,
                lastAlteration = null,
                lastVocabImport = null,
                lastVerify = null
            )
            db.getMangaDao().save(manga)
        }
    }

    @After
    fun tearDown() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val mockPath = File(context.cacheDir, "mock_mangas")
        if (mockPath.exists()) mockPath.deleteRecursively()
        db.close()
        DataBase.setTestingInstance(null)
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
        val timeout = 10000L
        while (System.currentTimeMillis() < startTime + timeout) {
            try {
                onView(withId(R.id.skeleton_layout)).check(
                    matches(
                        anyOf(
                            withEffectiveVisibility(Visibility.GONE),
                            withEffectiveVisibility(Visibility.INVISIBLE)
                        )
                    )
                )
                return
            } catch (e: Throwable) {
                Thread.sleep(200)
            }
        }
    }

    private fun waitForRecyclerViewData(recyclerViewId: Int, timeout: Long = 10000) {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() < startTime + timeout) {
            try {
                onView(withId(recyclerViewId)).check { view, noViewFoundException ->
                    if (noViewFoundException != null) throw noViewFoundException
                    val recyclerView = view as RecyclerView
                    val adapter = recyclerView.adapter
                    if (adapter == null || adapter.itemCount == 0) {
                        throw RuntimeException("RecyclerView is empty")
                    }
                    
                    // Garante que pelo menos um item foi desenhado na tela
                    if (recyclerView.childCount == 0) {
                        throw RuntimeException("No children attached to RecyclerView yet")
                    }
                }
                return
            } catch (e: Throwable) {
                Thread.sleep(200)
            }
        }
    }

    private fun clickChildViewWithId(id: Int): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> = allOf(isAssignableFrom(View::class.java), isDisplayed())
            override fun getDescription(): String = "Click on a child view with specified id."
            override fun perform(uiController: UiController, view: View) {
                val v = view.findViewById<View>(id)
                v.performClick()
            }
        }
    }

    @Test
    fun testMangaLibraryFullRoutine() {
        ActivityScenario.launch(MainActivity::class.java).use {
            Thread.sleep(waitTime)

            // 1-Seguir abrindo a biblioteca de manga
            onView(withId(R.id.drawer_layout)).perform(DrawerActions.open())
            onView(withId(R.id.nav_view)).perform(NavigationViewActions.navigateTo(R.id.menu_manga_library_default))
            
            // Espera a biblioteca carregar (skeleton sumir e lista aparecer)
            waitForSkeleton()
            waitForView(withId(R.id.manga_library_recycler_view))
            waitForRecyclerViewData(R.id.manga_library_recycler_view)
            Thread.sleep(waitTime)

            // Força o scroll/layout do primeiro item antes de interagir
            onView(withId(R.id.manga_library_recycler_view)).perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(0))
            Thread.sleep(waitTime)

            // 1.1-Selecionar como favorito um item
            // Usamos actionOnItem com matcher em vez de posição para maior estabilidade
            onView(withId(R.id.manga_library_recycler_view))
                .perform(RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                    hasDescendant(withId(R.id.manga_line_favorite)), 
                    clickChildViewWithId(R.id.manga_line_favorite)
                ))
            Thread.sleep(waitTime)

            // 1.2-Apagar outro item
            // Tentamos encontrar o item que tem o botão de config
            onView(withId(R.id.manga_library_recycler_view))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, clickChildViewWithId(R.id.manga_line_config)))
            Thread.sleep(waitTime)
            onView(withText(R.string.menu_manga_config_delete)).perform(click())
            Thread.sleep(waitTime)
            onView(withText(R.string.action_delete)).perform(click()) 
            Thread.sleep(waitTime)

            // 1.3-Marcar o item como lido (Usando Book Mark como rotina completa)
            onView(withId(R.id.manga_library_recycler_view))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, clickChildViewWithId(R.id.manga_line_config)))
            Thread.sleep(waitTime)
            onView(withText(R.string.menu_manga_config_book_mark)).perform(click())
            Thread.sleep(waitTime)
            // No popup de bookmark, apenas voltamos
            pressBack()
            Thread.sleep(waitTime)

            // 1.4-Limpar um item que estava com o lido
            onView(withId(R.id.manga_library_recycler_view))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, clickChildViewWithId(R.id.manga_line_config)))
            Thread.sleep(waitTime)
            onView(withText(R.string.menu_manga_config_clear_progress)).perform(click())
            Thread.sleep(waitTime)

            // 1.5-Filtrar a pesquisa do item por nome, adicionar autor
            onView(withId(R.id.menu_manga_library_search)).perform(click())
            onView(isAssignableFrom(android.widget.EditText::class.java)).perform(typeText("Manga @author:Fenix"), pressImeActionButton())
            Thread.sleep(waitTime)

            // 1.6-Limpar a filtragem
            onView(withId(androidx.appcompat.R.id.search_close_btn)).perform(click())
            Thread.sleep(waitTime)
            pressBack() 
            Thread.sleep(waitTime)

            // 1.7-Mudar a ordenação da lista
            onView(withId(R.id.menu_manga_library_list_order)).perform(click())
            Thread.sleep(waitTime)

            // 1.7-Abrir o popup de ordenação e mudar a ordem após isso selecionar apenas os favoritos
            onView(withId(R.id.menu_manga_library_list_order)).perform(longClick())
            Thread.sleep(waitTime)
            onView(withText(R.string.popup_library_manga_tab_item_filter)).perform(click())
            onView(withId(R.id.popup_library_filter_favorite)).perform(click())
            pressBack()
            Thread.sleep(waitTime)

            // 1.8-Abrir com o clique longo os detalhes, rolar um pouco a pagina e voltar para a biblioteca
            onView(withId(R.id.manga_library_recycler_view))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, longClick()))
            Thread.sleep(waitTime)
            onView(withId(R.id.manga_detail_scroll)).perform(swipeUp())
            Thread.sleep(waitTime)
            pressBack()
            Thread.sleep(waitTime)

            // 1.9-Abrir o item e esperar carregar a tela de leitura
            onView(withId(R.id.manga_library_recycler_view))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))
            Thread.sleep(waitTime)
            waitForView(withId(R.id.root_activity_manga_reader))
        }
    }
}
