package br.com.fenix.bilingualreader.view.ui.annotation

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.TestActivity
import br.com.fenix.bilingualreader.model.entity.*
import br.com.fenix.bilingualreader.model.enums.Libraries
import br.com.fenix.bilingualreader.model.enums.MarkType
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.repository.DataBase
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.view.ui.reader.book.BookReaderActivity
import br.com.fenix.bilingualreader.view.ui.reader.manga.MangaReaderActivity
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.containsString
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class AnnotationFragmentTest {

    private lateinit var db: DataBase

    private fun createMockLibrary(context: Context, type: Type, id: Long): Library {
        val folderName = if (type == Type.MANGA) "mock_mangas" else "mock_books"
        val mockPath = File(context.cacheDir, folderName)
        if (!mockPath.exists()) mockPath.mkdirs()

        return Library(
            id = id,
            title = if (type == Type.MANGA) "Manga Test Lib" else "Book Test Lib",
            path = mockPath.absolutePath,
            language = Libraries.JAPANESE,
            type = type
        )
    }

    @Before
    fun setup() {
        Intents.init()
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, DataBase::class.java)
            .allowMainThreadQueries()
            .build()
        
        DataBase.setTestingInstance(db)

        // Setup Libraries
        val mangaLib = createMockLibrary(context, Type.MANGA, 1L)
        val bookLib = createMockLibrary(context, Type.BOOK, 2L)
        db.getLibrariesDao().save(mangaLib)
        db.getLibrariesDao().save(bookLib)

        // Setup Manga
        val manga = Manga(mangaLib.id!!, 10L, File(mangaLib.path, "manga1.cbz")).apply {
            title = "Sample Manga"
        }
        db.getMangaDao().save(manga)

        // Setup Book
        val book = Book(20L, bookLib.id!!, "Sample Book", File(bookLib.path, "book1.epub")).apply {
            title = "Sample Book"
            pages = 100
        }
        db.getBookDao().save(book)

        // Setup Annotations
        val mangaAnnotation = MangaAnnotation(manga.id!!, "Chapter 1", "Note for Manga", "Page text").apply {
            page = 5
            id_parent = manga.id!!
            type = Type.MANGA
            markType = MarkType.Annotation
            alteration = LocalDateTime.now()
        }
        db.getMangaAnnotation().save(mangaAnnotation)

        val bookAnnotation = BookAnnotation(book.id!!, 1f, "Chapter 1", "Note for Book", "Sentence from book").apply {
            page = 10
            id_parent = book.id!!
            type = Type.BOOK
            markType = MarkType.Annotation
            alteration = LocalDateTime.now()
        }
        db.getBookAnnotation().save(bookAnnotation)
    }

    @After
    fun tearDown() {
        Intents.release()
        db.close()
    }

    @Test
    fun testAnnotationListIsDisplayed() {
        val scenario = ActivityScenario.launch(TestActivity::class.java)
        scenario.onActivity { activity ->
            val fragment = AnnotationFragment()
            activity.setFragment(fragment)
        }
        
        // Aguarda carregamento assíncrono do ViewModel
        Thread.sleep(2000)
        
        // Verifica se a lista está visível
        onView(withId(R.id.annotation_recycler_view)).check(matches(isDisplayed()))
        
        // Verifica se as anotações mockadas aparecem (pelo texto da nota ou capítulo)
        onView(withText("Note for Manga")).check(matches(isDisplayed()))
        onView(withText("Note for Book")).check(matches(isDisplayed()))
        
        // Verifica títulos de agrupamento (Nomes dos livros/mangás)
        onView(withText("Sample Manga")).check(matches(isDisplayed()))
        onView(withText("Sample Book")).check(matches(isDisplayed()))
    }

    @Test
    fun testSearchFunctionality() {
        val scenario = ActivityScenario.launch(TestActivity::class.java)
        scenario.onActivity { activity ->
            activity.setFragment(AnnotationFragment())
        }
        
        Thread.sleep(2000)

        // Clica no ícone de busca
        onView(withId(R.id.menu_annotation_search)).perform(click())
        
        // Digita algo que filtre apenas uma das anotações
        onView(withClassName(containsString("SearchView"))).perform(typeText("Manga"))
        
        // Aguarda debounce da busca
        Thread.sleep(1000)
        
        // Verifica se apenas a anotação de manga sobrou (ou a de livro sumiu)
        onView(withText("Note for Manga")).check(matches(isDisplayed()))
        // A anotação de livro não deve estar visível (ou nem existir na árvore se o filtro funcionou)
        // Como o recycler view remove os itens, verificamos se não existe
        onView(withText("Note for Book")).check(doesNotExist())
    }

    @Test
    fun testFilterPopupVisibility() {
        val scenario = ActivityScenario.launch(TestActivity::class.java)
        scenario.onActivity { activity ->
            activity.setFragment(AnnotationFragment())
        }
        
        Thread.sleep(2000)

        // Clica no botão de filtros no menu
        onView(withId(R.id.menu_annotation_filters)).perform(click())
        
        // Verifica se o BottomSheet (annotation_popup_filter) está visível
        onView(withId(R.id.annotation_popup_filter)).check(matches(isDisplayed()))
        
        // Verifica se as abas do TabLayout estão presentes
        onView(withText(R.string.annotation_tab_item_filter)).check(matches(isDisplayed()))
        onView(withText(R.string.annotation_tab_item_color)).check(matches(isDisplayed()))
        onView(withText(R.string.annotation_tab_item_chapters)).check(matches(isDisplayed()))
    }

    @Test
    fun testNavigationToMangaReader() {
        val scenario = ActivityScenario.launch(TestActivity::class.java)
        scenario.onActivity { activity ->
            activity.setFragment(AnnotationFragment())
        }
        
        Thread.sleep(2000)

        // Clica na anotação de mangá
        onView(withText("Note for Manga")).perform(click())
        
        // Verifica se abriu a MangaReaderActivity com os extras corretos
        intended(allOf(
            hasComponent(MangaReaderActivity::class.java.name),
            hasExtra(GeneralConsts.KEYS.MANGA.NAME, "Sample Manga")
        ))
    }

    @Test
    fun testNavigationToBookReader() {
        val scenario = ActivityScenario.launch(TestActivity::class.java)
        scenario.onActivity { activity ->
            activity.setFragment(AnnotationFragment())
        }
        
        Thread.sleep(2000)

        // Clica na anotação de livro
        onView(withText("Note for Book")).perform(click())
        
        // Verifica se abriu a BookReaderActivity
        intended(allOf(
            hasComponent(BookReaderActivity::class.java.name),
            hasExtra(GeneralConsts.KEYS.BOOK.NAME, "Sample Book")
        ))
    @Test
    fun testSwipeToDelete() {
        val scenario = ActivityScenario.launch(TestActivity::class.java)
        scenario.onActivity { activity ->
            activity.setFragment(AnnotationFragment())
        }
        
        Thread.sleep(2000)

        // Realiza o swipe em uma das anotações
        onView(withText("Note for Manga")).perform(swipeLeft())
        
        // Verifica se o diálogo de confirmação apareceu
        // O título definido no código é R.string.book_annotation_delete
        onView(withText(R.string.book_annotation_delete)).check(matches(isDisplayed()))
        
        // Clica no botão de deletar (R.string.action_delete)
        onView(withText(R.string.action_delete)).perform(click())
        
        // Aguarda animação e atualização
        Thread.sleep(1000)
        
        // Verifica se a anotação sumiu
        onView(withText("Note for Manga")).check(doesNotExist())
    }
}
