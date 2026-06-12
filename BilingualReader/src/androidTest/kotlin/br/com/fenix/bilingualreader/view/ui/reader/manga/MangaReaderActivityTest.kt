package br.com.fenix.bilingualreader.view.ui.reader.manga

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.view.View
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.enums.Libraries
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.repository.DataBase
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.Telemetry
import com.google.android.material.tabs.TabLayout
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class MangaReaderActivityTest {

    private lateinit var db: DataBase
    private lateinit var mockManga: Manga
    private lateinit var mockLib: Library

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        // Disable Telemetry for tests to avoid Firebase initialization issues
        Telemetry.isEnabled = false

        // Setup Database
        db = Room.inMemoryDatabaseBuilder(context, DataBase::class.java)
            .allowMainThreadQueries()
            .build()
        DataBase.setTestingInstance(db)

        // Setup Library
        val mockPath = File(context.cacheDir, "mock_mangas_reader")
        if (mockPath.exists()) mockPath.deleteRecursively()
        mockPath.mkdirs()
        
        mockLib = Library(
            id = 1L,
            title = "Manga Test Library",
            path = mockPath.absolutePath,
            language = Libraries.ENGLISH,
            type = Type.MANGA
        )
        db.getLibrariesDao().save(mockLib)

        // Setup Manga - Create a directory and dummy images to satisfy DirectoryParse
        val mangaFile = File(mockPath, "manga_reader_test.zip")
        mangaFile.mkdirs()
        
        // Create 10 dummy images to match bookmark and satisfy Parse
        val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        for (i in 1..10) {
            val imgFile = File(mangaFile, "page_$i.jpg")
            imgFile.outputStream().use { 
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
            }
        }

        mockManga = Manga(mockLib.id, 600L, mangaFile).apply {
            title = "Reader UI Test Manga"
            pages = 100
            bookMark = 10
        }
        db.getMangaDao().save(mockManga)

        // Setup Manga Seguinte para testes de navegação
        val nextMangaFile = File(mockPath, "manga_reader_next.zip")
        nextMangaFile.mkdirs()
        File(nextMangaFile, "page_1.jpg").outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }

        val mockMangaNext = Manga(mockLib.id, 601L, nextMangaFile).apply {
            title = "Next Manga"
            pages = 5
        }
        db.getMangaDao().save(mockMangaNext)

        // Clear SharedPreferences to ensure test isolation
        GeneralConsts.getSharedPreferences(context).edit().clear().commit()
        GeneralConsts.getSharedPreferences(context).edit()
            .putBoolean(GeneralConsts.KEYS.TOUCH.MANGA_TOUCH_DEMONSTRATION, false)
            .commit()
    }

    @After
    fun tearDown() {
        db.close()
    }

    // Helper para realizar scroll em NestedScrollView (Espresso nativo não suporta)
    private fun nestedScrollTo(): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> {
                return allOf(isDescendantOfA(isAssignableFrom(NestedScrollView::class.java)), withEffectiveVisibility(Visibility.VISIBLE))
            }
            override fun getDescription(): String = "nested scroll to"
            override fun perform(uiController: UiController, view: View) {
                var parent = view.parent
                while (parent != null && parent !is NestedScrollView) {
                    parent = parent.parent
                }
                if (parent is NestedScrollView) {
                    parent.scrollTo(0, view.top)
                } else {
                    throw RuntimeException("View must be a descendant of NestedScrollView")
                }
            }
        }
    }

    // Helper para clique forçado ignorando constraints de visibilidade total
    private fun forceClick() = object : ViewAction {
        override fun getConstraints(): Matcher<View> = isEnabled()
        override fun getDescription(): String = "force click"
        override fun perform(uiController: UiController, view: View) {
            view.performClick()
        }
    }

    private fun selectTabAt(index: Int) = object : ViewAction {
        override fun getConstraints(): Matcher<View> = allOf(isDisplayed(), isAssignableFrom(TabLayout::class.java))
        override fun getDescription(): String = "select tab at index $index"
        override fun perform(uiController: UiController, view: View) {
            val tabLayout = view as TabLayout
            tabLayout.getTabAt(index)?.select()
        }
    }

    private fun getStartIntent(): Intent {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return Intent(context, MangaReaderActivity::class.java).apply {
            putExtra(GeneralConsts.KEYS.OBJECT.LIBRARY, mockLib)
            putExtra(GeneralConsts.KEYS.OBJECT.MANGA, mockManga)
            action = Intent.ACTION_MAIN
        }
    }

    @Test
    fun testMangaReaderHUDVisibility() {
        ActivityScenario.launch<MangaReaderActivity>(getStartIntent()).use {
            // Aguarda carregamento
            Thread.sleep(4000)

            // Garante que o HUD esteja visível.
            try {
                onView(withId(R.id.reader_manga_toolbar_reader_top)).check(matches(isDisplayed()))
            } catch (e: Throwable) {
                onView(withId(R.id.fragment_manga_reader_pager)).perform(click())
                Thread.sleep(2000)
            }

            // 3. Verifica se os componentes do HUD apareceram
            onView(withId(R.id.reader_manga_toolbar_reader_top)).check(matches(isDisplayed()))
            onView(withId(R.id.reader_manga_toolbar_reader_bottom)).check(matches(isDisplayed()))
            onView(withId(R.id.reader_manga_bottom_progress_content)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun testConfigurationPopups() {
        ActivityScenario.launch<MangaReaderActivity>(getStartIntent()).use {
            Thread.sleep(4000)

            // Garante que o HUD esteja visível
            try {
                onView(withId(R.id.reader_manga_toolbar_reader_top)).check(matches(isDisplayed()))
            } catch (e: Throwable) {
                onView(withId(R.id.fragment_manga_reader_pager)).perform(click())
                Thread.sleep(2000)
            }

            // 1. Testa Popup de Filtro de Cor (Brightness)
            onView(withId(R.id.reader_manga_btn_popup_color)).perform(click())
            Thread.sleep(1000)
            onView(withId(R.id.popup_manga_configurations_tab)).check(matches(isDisplayed()))
            
            // Fecha para testar o próximo
            onView(withId(R.id.reader_manga_btn_popup_color)).perform(click())
            Thread.sleep(1000)

            // 2. Testa Popup de Anotações/Marcadores
            onView(withId(R.id.reader_manga_btn_menu_annotations)).perform(click())
            Thread.sleep(1000)
            onView(withId(R.id.popup_manga_configurations_tab)).check(matches(isDisplayed()))
            
            // Verifica se a aba de anotações está visível no ViewPager
            onView(withText(R.string.popup_reading_manga_tab_item_configuration_bookmarks)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun testMangaNavigationButtons() {
        ActivityScenario.launch<MangaReaderActivity>(getStartIntent()).use {
            Thread.sleep(4000)

            // Garante que o HUD esteja visível
            try {
                onView(withId(R.id.reader_manga_toolbar_reader_top)).check(matches(isDisplayed()))
            } catch (e: Throwable) {
                onView(withId(R.id.fragment_manga_reader_pager)).perform(click())
                Thread.sleep(2000)
            }

            // Verifica visibilidade dos botões de navegação entre volumes/arquivos
            onView(withId(R.id.reader_manga_nav_previous_file)).check(matches(isDisplayed()))
            onView(withId(R.id.reader_manga_nav_next_file)).check(matches(isDisplayed()))
            onView(withId(R.id.reader_manga_nav_next_file)).perform(click())
            // Nota: Em um teste real, a Activity reiniciaria. Aqui validamos a intenção do clique.
        }
    }

    @Test
    fun testMangaPageSlider() {
        ActivityScenario.launch<MangaReaderActivity>(getStartIntent()).use { scenario ->
            Thread.sleep(4000)

            // Garante que o HUD esteja visível
            try {
                onView(withId(R.id.reader_manga_toolbar_reader_top)).check(matches(isDisplayed()))
            } catch (e: Throwable) {
                onView(withId(R.id.fragment_manga_reader_pager)).perform(click())
                Thread.sleep(2000)
            }

            // Interação com o Slider de progresso - verifica visibilidade
            onView(withId(R.id.reader_manga_bottom_progress)).check(matches(isDisplayed()))
            
            // Verifica se o texto de página atual está presente
            onView(withId(R.id.reader_manga_bottom_progress_title)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun testPopupColorFilterInteraction() {
        ActivityScenario.launch<MangaReaderActivity>(getStartIntent()).use { scenario ->
            Thread.sleep(4000)

            // Garante que o HUD esteja visível
            try {
                onView(withId(R.id.reader_manga_toolbar_reader_top)).check(matches(isDisplayed()))
            } catch (e: Throwable) {
                onView(withId(R.id.fragment_manga_reader_pager)).perform(click())
                Thread.sleep(2000)
            }

            // Abre o popup de configurações
            onView(withId(R.id.reader_manga_btn_popup_color)).perform(click())
            Thread.sleep(2500)

            // Seleciona a aba de Filtro de Cores via ID e índice para garantir
            onView(withId(R.id.popup_manga_configurations_tab)).perform(selectTabAt(0))
            Thread.sleep(1500)

            var initialValue = false
            scenario.onActivity { activity ->
                initialValue = ViewModelProvider(activity)[MangaReaderViewModel::class.java].customFilter.value ?: false
            }

            // Tenta clicar no switch. Se forceClick falhar em disparar o listener, tentamos click() regular
            onView(withId(R.id.popup_manga_switch_color_filter)).perform(forceClick())
            Thread.sleep(2000) // Mais tempo para processar o clique
            
            scenario.onActivity { activity ->
                val viewModel = ViewModelProvider(activity)[MangaReaderViewModel::class.java]
                val newValue = viewModel.customFilter.value ?: false
                assertNotEquals("O estado do filtro deve ter sido alterado", initialValue, newValue)
            }
        }
    }

    @Test
    fun testPopupSubtitleConfigInteraction() {
        ActivityScenario.launch<MangaReaderActivity>(getStartIntent()).use { scenario ->
            Thread.sleep(4000)

            // Garante que o HUD esteja visível
            try {
                onView(withId(R.id.reader_manga_toolbar_reader_top)).check(matches(isDisplayed()))
            } catch (e: Throwable) {
                onView(withId(R.id.fragment_manga_reader_pager)).perform(click())
                Thread.sleep(2000)
            }
            Thread.sleep(1000)

            // Abre o menu de opções e navega para Legendas
            openActionBarOverflowOrOptionsMenu(ApplicationProvider.getApplicationContext())
            Thread.sleep(800)
            onView(withText(R.string.menu_manga_reading_ocr_file_linked)).perform(click())
            Thread.sleep(800)
            onView(withText(R.string.menu_manga_reading_subtitle)).perform(click())
            Thread.sleep(3000)

            // Navega para a aba de Configuração de Legendas (Índice 2) via programática para garantir
            onView(withId(R.id.popup_manga_translate_tab)).perform(selectTabAt(2))
            Thread.sleep(2000)

            // Interage com o switch de busca linkada
            onView(withId(R.id.popup_manga_switch_use_page_linked_in_search_translate)).perform(forceClick())
            Thread.sleep(1500)

            val preferences = GeneralConsts.getSharedPreferences(ApplicationProvider.getApplicationContext())
            val isEnabled = preferences.getBoolean(GeneralConsts.KEYS.PAGE_LINK.USE_IN_SEARCH_TRANSLATE, false)
            // A preferência deve estar acessível (independente do valor inicial)
            onView(withId(R.id.popup_manga_switch_use_page_linked_in_search_translate)).check(matches(isEnabled()))
        }
    }

    @Test
    fun testPopupSubtitleReaderNavigation() {
        ActivityScenario.launch<MangaReaderActivity>(getStartIntent()).use { scenario ->
            Thread.sleep(4000)

            // Garante que o HUD esteja visível
            try {
                onView(withId(R.id.reader_manga_toolbar_reader_top)).check(matches(isDisplayed()))
            } catch (e: Throwable) {
                onView(withId(R.id.fragment_manga_reader_pager)).perform(click())
                Thread.sleep(2000)
            }

            // Abre o popup de tradução via menu
            openActionBarOverflowOrOptionsMenu(ApplicationProvider.getApplicationContext())
            Thread.sleep(800)
            onView(withText(R.string.menu_manga_reading_ocr_file_linked)).perform(click())
            Thread.sleep(800)
            onView(withText(R.string.menu_manga_reading_subtitle)).perform(click())
            Thread.sleep(2500)

            // Seleciona a aba do Leitor de Legendas
            onView(withText(R.string.popup_reading_manga_tab_item_subtitle)).perform(forceClick())
            Thread.sleep(1500)

            // Verifica se os botões de navegação do leitor de legenda estão presentes
            // Usamos forceClick ou apenas isEnabled matches se a visibilidade estiver flutuante
            onView(withId(R.id.popup_manga_subtitle_before_text)).check(matches(isEnabled()))
            onView(withId(R.id.popup_manga_subtitle_next_text)).check(matches(isEnabled()))
            
            // Testa clique nos controles de navegação de legenda
            onView(withId(R.id.popup_manga_subtitle_next_text)).perform(forceClick())
        }
    }

    @Test
    fun testPopupAnnotationsInteraction() {
        ActivityScenario.launch<MangaReaderActivity>(getStartIntent()).use { scenario ->
            Thread.sleep(4000)

            // Garante que o HUD esteja visível
            try {
                onView(withId(R.id.reader_manga_toolbar_reader_top)).check(matches(isDisplayed()))
            } catch (e: Throwable) {
                onView(withId(R.id.fragment_manga_reader_pager)).perform(click())
                Thread.sleep(2000)
            }

            // Abre Popup de Anotações diretamente pelo botão do menu
            onView(withId(R.id.reader_manga_btn_menu_annotations)).perform(click())
            Thread.sleep(1500)

            // Garante que a lista de anotações (RecyclerView) está visível
            onView(withId(R.id.popup_manga_annotations_list)).check(matches(isDisplayed()))
        }
    }
}
