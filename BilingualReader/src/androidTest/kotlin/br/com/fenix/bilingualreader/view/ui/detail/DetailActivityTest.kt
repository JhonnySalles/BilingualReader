package br.com.fenix.bilingualreader.view.ui.detail

import android.content.Context
import android.content.Intent
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.enums.Libraries
import br.com.fenix.bilingualreader.model.enums.Themes
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.repository.DataBase
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class DetailActivityTest {

    private lateinit var db: DataBase
    private lateinit var mockLib: Library

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        // Setup Database em memória para evitar crashes no ciclo de vida do ViewModel dos Fragments
        db = Room.inMemoryDatabaseBuilder(context, DataBase::class.java)
            .allowMainThreadQueries()
            .build()
        DataBase.setTestingInstance(db)

        val mockPath = File(context.cacheDir, "mock_details_act")
        if (!mockPath.exists()) mockPath.mkdirs()
        
        mockLib = Library(
            id = 1L,
            title = "Test Library Routing",
            path = mockPath.absolutePath,
            language = Libraries.ENGLISH,
            type = Type.BOOK
        )
        db.getLibrariesDao().save(mockLib)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testDetailActivityRoutesToMangaFragment() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val mangaFile = File(context.cacheDir, "test_routing.zip")
        if (!mangaFile.exists()) mangaFile.createNewFile()
        
        val mockManga = Manga(mockLib.id, 100L, mangaFile).apply {
            title = "Test Manga Routing"
            name = "Test Manga Routing"
            pages = 100
        }
        db.getMangaDao().save(mockManga)

        val intent = Intent(context, DetailActivity::class.java).apply {
            putExtra(GeneralConsts.KEYS.OBJECT.LIBRARY, mockLib)
            putExtra(GeneralConsts.KEYS.OBJECT.MANGA, mockManga)
        }

        // Lança DetailActivity com os parâmetros do Manga e averigua o sub-fragmento
        ActivityScenario.launch<DetailActivity>(intent)

        Thread.sleep(1000)

        // Verifica se a scroll view exclusiva do MangaDetailFragment está visível
        onView(withId(R.id.manga_detail_scroll)).check(matches(isDisplayed()))
    }

    @Test
    fun testDefaultToMangaFragment() {
        // Sem extras, deve carregar MangaDetailFragment por padrão conforme lógica no onCreate()
        ActivityScenario.launch<DetailActivity>(Intent(ApplicationProvider.getApplicationContext(), DetailActivity::class.java))

        Thread.sleep(1000)
        
        onView(withId(R.id.manga_detail_scroll)).check(matches(isDisplayed()))
    }

    @Test
    fun testBackNavigation() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val bookFile = File(context.cacheDir, "test_back.epub")
        if (!bookFile.exists()) bookFile.createNewFile()
        
        val mockBook = Book(mockLib.id, 300L, bookFile).apply {
            title = "Test Back Navigation"
            pages = 100
        }
        db.getBookDao().save(mockBook)

        val intent = Intent(context, DetailActivity::class.java).apply {
            putExtra(GeneralConsts.KEYS.OBJECT.LIBRARY, mockLib)
            putExtra(GeneralConsts.KEYS.OBJECT.BOOK, mockBook)
        }

        val scenario = ActivityScenario.launch<DetailActivity>(intent)
        Thread.sleep(1500)

        // Tenta clicar no botão de voltar da Toolbar (Home) de forma programática para evitar falhas de foco no AVD
        scenario.onActivity { it.onBackPressed() }

        Thread.sleep(2000)

        // Verifica se a activity foi finalizada (Estado DESTROYED)
        assertEquals(androidx.lifecycle.Lifecycle.State.DESTROYED, scenario.state)
    }

    @Test
    fun testHardwareBackNavigation() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val bookFile = File(context.cacheDir, "test_hw_back.epub")
        if (!bookFile.exists()) bookFile.createNewFile()
        
        val mockBook = Book(mockLib.id, 400L, bookFile).apply {
            title = "Test HW Back"
            pages = 100
        }
        db.getBookDao().save(mockBook)

        val intent = Intent(context, DetailActivity::class.java).apply {
            putExtra(GeneralConsts.KEYS.OBJECT.LIBRARY, mockLib)
            putExtra(GeneralConsts.KEYS.OBJECT.BOOK, mockBook)
        }

        val scenario = ActivityScenario.launch<DetailActivity>(intent)
        Thread.sleep(1500)

        // Simula o botão de voltar de forma programática
        scenario.onActivity { it.onBackPressed() }
        Thread.sleep(2000)

        // Verifica se a activity foi finalizada
        assertEquals(androidx.lifecycle.Lifecycle.State.DESTROYED, scenario.state)
    }

    @Test
    fun testConfigurationChangeStability() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val mangaFile = File(context.cacheDir, "test_rotation.zip")
        if (!mangaFile.exists()) mangaFile.createNewFile()
        
        val mockManga = Manga(mockLib.id, 500L, mangaFile).apply {
            title = "Rotation Test Manga"
            name = "Rotation Test Manga"
            pages = 150
        }
        db.getMangaDao().save(mockManga)

        val intent = Intent(context, DetailActivity::class.java).apply {
            putExtra(GeneralConsts.KEYS.OBJECT.LIBRARY, mockLib)
            putExtra(GeneralConsts.KEYS.OBJECT.MANGA, mockManga)
        }

        val scenario = ActivityScenario.launch<DetailActivity>(intent)
        Thread.sleep(2000)

        // Verifica se o fragmento do manga está lá
        onView(withId(R.id.manga_detail_scroll)).check(matches(isDisplayed()))

        // Simula rotação/mudança de configuração
        scenario.recreate()
        Thread.sleep(3000)

        // Verifica se, após a recriação, o fragmento correto foi restaurado e continua visível
        onView(withId(R.id.manga_detail_scroll)).check(matches(isDisplayed()))
        onView(withId(R.id.manga_detail_title)).check(matches(withText("Rotation Test Manga")))
    }

    @Test
    fun testThemeStability() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        // Define um tema diferente no SharedPreferences antes de iniciar
        GeneralConsts.getSharedPreferences(context).edit()
            .putString(GeneralConsts.KEYS.THEME.THEME_USED, Themes.BLUE.toString())
            .commit()

        val mangaFile = File(context.cacheDir, "test_theme.zip")
        if (!mangaFile.exists()) mangaFile.createNewFile()
        val mockManga = Manga(mockLib.id, 600L, mangaFile).apply { title = "Theme Test" }
        db.getMangaDao().save(mockManga)

        val intent = Intent(context, DetailActivity::class.java).apply {
            putExtra(GeneralConsts.KEYS.OBJECT.LIBRARY, mockLib)
            putExtra(GeneralConsts.KEYS.OBJECT.MANGA, mockManga)
        }

        // Lança e verifica se não há crash
        ActivityScenario.launch<DetailActivity>(intent)
        Thread.sleep(1000)
        onView(withId(R.id.manga_detail_scroll)).check(matches(isDisplayed()))
        
        // Retorna ao tema original para não afetar outros testes
        GeneralConsts.getSharedPreferences(context).edit()
            .putString(GeneralConsts.KEYS.THEME.THEME_USED, Themes.ORIGINAL.toString())
            .commit()
    }

    @Test
    fun testDetailActivityRoutesToBookFragment() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val bookFile = File(context.cacheDir, "test_routing.epub")
        if (!bookFile.exists()) bookFile.createNewFile()
        
        val mockBook = Book(mockLib.id, 200L, bookFile).apply {
            title = "Test Book Routing"
            pages = 200
        }
        db.getBookDao().save(mockBook)

        val intent = Intent(context, DetailActivity::class.java).apply {
            putExtra(GeneralConsts.KEYS.OBJECT.LIBRARY, mockLib)
            putExtra(GeneralConsts.KEYS.OBJECT.BOOK, mockBook)
        }

        // Lança DetailActivity com os parâmetros do Book e averigua o sub-fragmento
        ActivityScenario.launch<DetailActivity>(intent)

        Thread.sleep(1000)

        // Verifica se a scroll view exclusiva do BookDetailFragment está visível
        onView(withId(R.id.book_detail_scroll)).check(matches(isDisplayed()))
    }
}
