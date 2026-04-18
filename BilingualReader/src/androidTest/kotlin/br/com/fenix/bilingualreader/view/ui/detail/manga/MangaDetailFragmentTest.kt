package br.com.fenix.bilingualreader.view.ui.detail.manga

import android.content.Context
import android.content.Intent
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
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
import br.com.fenix.bilingualreader.view.ui.detail.DetailActivity
import br.com.fenix.bilingualreader.view.ui.vocabulary.VocabularyActivity
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.containsString
import java.time.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class MangaDetailFragmentTest {

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
        val mockPath = File(context.cacheDir, "mock_mangas_detail")
        if (!mockPath.exists()) mockPath.mkdirs()
        
        mockLib = Library(
            id = 1L,
            title = "Test Library Manga",
            path = mockPath.absolutePath,
            language = Libraries.ENGLISH,
            type = Type.MANGA
        )
        db.getLibrariesDao().save(mockLib)

        // Setup Manga
        val mangaFile = File(mockPath, "manga_test.zip")
        if (!mangaFile.exists()) mangaFile.createNewFile()

        mockManga = Manga(mockLib.id, 100L, mangaFile).apply {
            title = "Detail Test Manga"
            pages = 200
            bookMark = 20
            favorite = false
        }
        db.getMangaDao().save(mockManga)

        Intents.init()
    }

    @After
    fun tearDown() {
        Intents.release()
        db.close()
    }

    private fun getStartIntent(): Intent {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return Intent(context, DetailActivity::class.java).apply {
            putExtra(GeneralConsts.KEYS.OBJECT.LIBRARY, mockLib)
            putExtra(GeneralConsts.KEYS.OBJECT.MANGA, mockManga)
        }
    }

    @Test
    fun testMangaDetailDisplay() {
        ActivityScenario.launch<DetailActivity>(getStartIntent())
        
        // Aguarda carregamento do ViewModel e renderização inicial
        Thread.sleep(2000)

        // Verifica os campos principais
        onView(withId(R.id.manga_detail_title)).check(matches(withText(mockManga.name)))
        onView(withId(R.id.manga_detail_folder)).check(matches(withText(mockManga.path)))
        
        // Verifica o progresso (texto)
        // O formato no código é: "${it.bookMark} / ${it.pages}"
        onView(withId(R.id.manga_detail_book_mark)).check(matches(withText("${mockManga.bookMark} / ${mockManga.pages}")))
    }

    @Test
    fun testMangaFullInformationDisplay() {
        // Popula o mock com dados detalhados para validar a aba de informações locais
        mockManga.apply {
            author = "Mangaka de Teste"
            series = "Série Épica de Teste"
            volume = "22"
            publisher = "Editora Planeta Manga"
            genre = "Seinen, Psicológico"
            release = LocalDate.of(2025, 1, 1)
        }
        db.getMangaDao().save(mockManga)

        ActivityScenario.launch<DetailActivity>(getStartIntent())
        
        Thread.sleep(2500)

        // Valida campos de Informação Local
        onView(withId(R.id.manga_detail_local_information_authors))
            .perform(scrollTo())
            .check(matches(withText(containsString("Mangaka de Teste"))))

        onView(withId(R.id.manga_detail_local_information_series))
            .perform(scrollTo())
            .check(matches(withText(containsString("Série Épica de Teste"))))

        onView(withId(R.id.manga_detail_local_information_volume))
            .perform(scrollTo())
            .check(matches(withText(containsString("22"))))

        onView(withId(R.id.manga_detail_local_information_publisher))
            .perform(scrollTo())
            .check(matches(withText(containsString("Editora Planeta Manga"))))

        onView(withId(R.id.manga_detail_local_information_release))
            .perform(scrollTo())
            .check(matches(withText(containsString("2025"))))
    }

    @Test
    fun testFavoriteToggle() {
        ActivityScenario.launch<DetailActivity>(getStartIntent())
        
        Thread.sleep(2000)

        // Inicialmente não é favorito no DB
        assertFalse(db.getMangaDao().get(mockManga.id!!)!!.favorite)

        // Clica no botão de favorito
        onView(withId(R.id.manga_detail_button_favorite)).perform(click())
        
        // Aguarda persistência assíncrona
        Thread.sleep(500)
        
        // Verifica se o valor mudou no Banco de Dados
        assertTrue("O estado de favorito não foi persistido no Banco de Dados", 
            db.getMangaDao().get(mockManga.id!!)!!.favorite)
            
        // Clica novamente para desmarcar
        onView(withId(R.id.manga_detail_button_favorite)).perform(click())
        Thread.sleep(500)
        assertFalse(db.getMangaDao().get(mockManga.id!!)!!.favorite)
    }

    @Test
    fun testDeleteDialogAppearance() {
        ActivityScenario.launch<DetailActivity>(getStartIntent())
        
        Thread.sleep(2000)

        // Clica no botão de deletar
        onView(withId(R.id.manga_detail_button_delete)).perform(scrollTo(), click())
        
        // Verifica se o diálogo de confirmação apareceu
        onView(withText(R.string.manga_library_menu_delete)).check(matches(isDisplayed()))
        
        // Cancela a ação
        onView(withText(R.string.action_negative)).perform(click())
        
        // Verifica se o diálogo sumiu
        Thread.sleep(500)
        onView(withText(R.string.manga_library_menu_delete)).check(doesNotExist())
    }

    @Test
    fun testMarkReadButton() {
        ActivityScenario.launch<DetailActivity>(getStartIntent())
        Thread.sleep(2000)

        // Clica no botão de marcar como lido
        onView(withId(R.id.manga_detail_button_mark_read)).perform(scrollTo(), click())
        
        Thread.sleep(500)
        
        // No repositório, markRead define bookMark = pages
        val updatedManga = db.getMangaDao().get(mockManga.id!!)!!
        assertEquals(updatedManga.pages, updatedManga.bookMark)
    }

    @Test
    fun testVocabularyNavigation() {
        ActivityScenario.launch<DetailActivity>(getStartIntent())
        Thread.sleep(2000)

        // Clica no botão de Vocabulário
        onView(withId(R.id.manga_detail_button_vocabulary)).perform(scrollTo(), click())

        // Verifica se a intent para VocabularyActivity foi disparada
        intended(allOf(
            hasComponent(VocabularyActivity::class.java.name),
            hasExtra(GeneralConsts.KEYS.VOCABULARY.TYPE, Type.MANGA)
        ))
    }

    @Test
    fun testComicInfoTagsDisplay() {
        // Popula o manga mock com um layout de gênero que servirá para criação de Tags na view
        mockManga.genre = "Isekai, Comedy"
        db.getMangaDao().save(mockManga)
        
        ActivityScenario.launch<DetailActivity>(getStartIntent())
        Thread.sleep(2000)

        // O fragment popula os cards em manga_detail_local_information_comic_info_tags convertendo genre via ListUtil
        onView(withId(R.id.manga_detail_local_information_comic_info_tags))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
            
        onView(withText("Isekai")).check(matches(isDisplayed()))
    }

    @Test
    fun testCoverPopup() {
        ActivityScenario.launch<DetailActivity>(getStartIntent())
        Thread.sleep(2000)

        // Clica na imagem de capa
        onView(withId(R.id.manga_detail_manga_image)).perform(click())

        // Verifica se o ImageView do popup subiu
        onView(withId(R.id.popup_detail_image))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
    }
    
}
