package br.com.fenix.bilingualreader.view.ui.reader.manga

import android.content.Context
import android.content.Intent
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
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
import br.com.fenix.bilingualreader.service.repository.DataBase
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import org.junit.After
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
        
        // Setup Database
        db = Room.inMemoryDatabaseBuilder(context, DataBase::class.java)
            .allowMainThreadQueries()
            .build()
        DataBase.setTestingInstance(db)

        // Setup Library
        val mockPath = File(context.cacheDir, "mock_mangas_reader")
        if (!mockPath.exists()) mockPath.mkdirs()
        
        mockLib = Library(
            id = 1L,
            title = "Manga Test Library",
            path = mockPath.absolutePath,
            language = Libraries.ENGLISH,
            type = Type.MANGA
        )
        db.getLibrariesDao().save(mockLib)

        // Setup Manga
        val mangaFile = File(mockPath, "manga_reader_test.zip")
        if (!mangaFile.exists()) mangaFile.createNewFile()

        mockManga = Manga(mockLib.id, 600L, mangaFile).apply {
            title = "Reader UI Test Manga"
            pages = 100
            bookMark = 10
        }
        db.getMangaDao().save(mockManga)
    }

    @After
    fun tearDown() {
        db.close()
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
        ActivityScenario.launch<MangaReaderActivity>(getStartIntent())
        
        // Aguarda transição e renderização (ViewModel + HUD)
        Thread.sleep(2000)

        // Verifica o título na Toolbar (Nativo ou Custom)
        onView(withText(mockManga.name)).check(matches(isDisplayed()))
        
        // Verifica o texto de progresso no rodapé
        // Formato no código: getString(R.string.progress, page, mManga!!.pages)
        // No setup definimos bookMark = 10 e pages = 100
        onView(withId(R.id.reader_manga_bottom_progress_title)).check(matches(withText("${mockManga.bookMark} / ${mockManga.pages}")))
        
        // Verifica a presença do indicador de linguagem OCR
        onView(withId(R.id.reader_manga_ocr_language)).check(matches(isDisplayed()))
    }

    @Test
    fun testConfigurationPopups() {
        ActivityScenario.launch<MangaReaderActivity>(getStartIntent())
        
        Thread.sleep(2000)

        // 1. Testa Popup de Filtro de Cor (Brightness)
        onView(withId(R.id.reader_manga_btn_popup_color)).perform(click())
        Thread.sleep(1000)
        onView(withId(R.id.popup_manga_configurations_tab)).check(matches(isDisplayed()))
        
        // Fecha para testar o próximo (clicando fora ou no botão novamente)
        onView(withId(R.id.reader_manga_btn_popup_color)).perform(click())
        Thread.sleep(500)

        // 2. Testa Popup de Anotações/Marcadores
        onView(withId(R.id.reader_manga_btn_menu_annotations)).perform(click())
        Thread.sleep(1000)
        onView(withId(R.id.popup_manga_configurations_tab)).check(matches(isDisplayed()))
        
        // Verifica se a aba de anotações está visível no ViewPager
        onView(withText(R.string.popup_reading_manga_tab_item_configuration_bookmarks)).check(matches(isDisplayed()))
    }

    @Test
    fun testMangaNavigationButtons() {
        ActivityScenario.launch<MangaReaderActivity>(getStartIntent())
        
        Thread.sleep(2000)

        // Verifica existência dos botões de navegação entre volumes/arquivos
        onView(withId(R.id.reader_manga_nav_previous_file)).check(matches(isDisplayed()))
        onView(withId(R.id.reader_manga_nav_next_file)).check(matches(isDisplayed()))
    }
}
