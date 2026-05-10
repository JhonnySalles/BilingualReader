package br.com.fenix.bilingualreader.view.ui.vocabulary.manga

import android.content.Context
import android.content.Intent
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.entity.Vocabulary
import br.com.fenix.bilingualreader.model.enums.Libraries
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.repository.DataBase
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.Util
import br.com.fenix.bilingualreader.view.ui.vocabulary.VocabularyActivity
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class VocabularyMangaFragmentTest {

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

        // Setup Library & Manga file para evitar NPE nas capas
        val mockPath = File(context.cacheDir, "mock_manga_vocab")
        if (!mockPath.exists()) mockPath.mkdirs()
        mockLib = Library(1L, "Vocab Manga Lib", mockPath.absolutePath, Libraries.JAPANESE, Type.MANGA)
        db.getLibrariesDao().save(mockLib)

        val mangaFile = File(mockPath, "vocab_manga.zip")
        if (!mangaFile.exists()) mangaFile.createNewFile()
        mockManga = Manga(mockLib.id, 500L, mangaFile)

        // Injeta dados de vocabulário associados ao mangá
        val mangaId = db.getMangaDao().save(mockManga)
        val vocabId = db.getVocabularyDao().save(Vocabulary(id = 10L, word = "MangaWord", reading = "ReadingM", english = "MeaningM", portuguese = null, basicForm = null, jlpt = 0, revised = false, favorite = false, appears = 1))
        db.getVocabularyDao().insert(db.openHelper, mangaId, vocabId, 1, true)


    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun getStartIntent(): Intent {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return Intent(context, VocabularyActivity::class.java).apply {
            putExtra(GeneralConsts.KEYS.VOCABULARY.TYPE, Type.MANGA)
            putExtra(GeneralConsts.KEYS.OBJECT.MANGA, mockManga)
            action = Intent.ACTION_MAIN
        }
    }

    @Test
    fun testVocabularyMangaRendering() {
        ActivityScenario.launch<VocabularyActivity>(getStartIntent())
        
        Thread.sleep(3500)

        // Verifica se o título do mangá foi injetado no EditText da busca contextual
        // O fragmento exibe o .title (sem extensão) e não o .name
        onView(withId(R.id.vocabulary_manga_edittext)).check(matches(withText(mockManga.title)))
        
        // Verifica se a palavra associada a este mangá aparece na lista (verticalizada)
        onView(withText(Util.setVerticalText("MangaWord"))).check(matches(isDisplayed()))
    }

    @Test
    fun testOrderBottomSheetTrigger() {
        ActivityScenario.launch<VocabularyActivity>(getStartIntent())
        
        Thread.sleep(2000)

        // Clique longo no menu de ordem
        onView(withId(R.id.menu_vocabulary_list_order)).perform(longClick())
        
        Thread.sleep(1000)
        
        // Verifica se o BottomSheet respectivo ao Manga apareceu
        onView(withId(R.id.vocabulary_manga_popup_menu_order_filter)).check(matches(isDisplayed()))
    }

    @Test
    fun testMangaTitleSearchChange() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val mockPath = File(context.cacheDir, "mock_manga_vocab")
        
        // Setup segundo mangá
        val mangaFile2 = File(mockPath, "second_manga.zip")
        if (!mangaFile2.exists()) mangaFile2.createNewFile()
        val mockManga2 = Manga(mockLib.id, 501L, mangaFile2)
        val mangaId2 = db.getMangaDao().save(mockManga2)
        
        val vocabId2 = db.getVocabularyDao().save(Vocabulary(id = 11L, word = "SecondWord", reading = "ReadingS", english = "MeaningS", portuguese = null, basicForm = null, jlpt = 0, revised = false, favorite = false, appears = 1))
        db.getVocabularyDao().insert(db.openHelper, mangaId2, vocabId2, 1, true)

        ActivityScenario.launch<VocabularyActivity>(getStartIntent())
        
        Thread.sleep(3500)

        // Limpa e digita o novo título no EditText contextual
        onView(withId(R.id.vocabulary_manga_edittext)).perform(replaceText(mockManga2.title))
        
        // Aguarda o TextWatcher (1000ms) + carregamento
        Thread.sleep(2500)
        
        // Verifica se a palavra associada ao NOVO mangá aparece
        onView(withText(Util.setVerticalText("SecondWord"))).check(matches(isDisplayed()))
    }
}
