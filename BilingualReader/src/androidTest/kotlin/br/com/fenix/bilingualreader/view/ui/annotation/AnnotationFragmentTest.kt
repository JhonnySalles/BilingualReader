package br.com.fenix.bilingualreader.view.ui.annotation

import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.swipeLeft
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.TestActivity
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.BookAnnotation
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.entity.MangaAnnotation
import br.com.fenix.bilingualreader.model.enums.Libraries
import br.com.fenix.bilingualreader.model.enums.MarkType
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.repository.DataBase
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.view.ui.reader.book.BookReaderActivity
import br.com.fenix.bilingualreader.view.ui.reader.manga.MangaReaderActivity
import org.hamcrest.Matchers.allOf
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
        val mangaFile = File(mangaLib.path, "manga1.cbz")
        if (!mangaFile.exists()) mangaFile.createNewFile()
        val manga = Manga(fkLibrary = mangaLib.id!!, id = 10L, file = mangaFile).apply {
            title = "Sample Manga"
        }
        db.getMangaDao().save(manga)

        // Setup Book
        val bookFile = File(bookLib.path, "book1.epub")
        if (!bookFile.exists()) bookFile.createNewFile()
        val book = Book(fkLibrary = bookLib.id!!, id = 20L, file = bookFile).apply {
            title = "Sample Book"
            pages = 100
        }
        db.getBookDao().save(book)

        // Setup Annotations
        val mangaAnnotation = MangaAnnotation(
            id = null,
            id_parent = manga.id!!,
            page = 5,
            pages = 10,
            markType = MarkType.Annotation,
            chapter = "Manga Chapter",
            folder = "Note for Manga",
            annotation = "Page text",
            alteration = LocalDateTime.now(),
            created = LocalDateTime.now()
        )
        db.getMangaAnnotation().save(mangaAnnotation)

        val bookAnnotation = BookAnnotation(
            id = null,
            id_parent = book.id!!,
            page = 10,
            pages = 100,
            fontSize = 12f,
            markType = MarkType.Annotation,
            chapterNumber = 1f,
            chapter = "Book Chapter",
            text = "Sentence from book",
            range = intArrayOf(0, 10),
            annotation = "Note for Book",
            favorite = false,
            color = br.com.fenix.bilingualreader.model.enums.Color.None,
            alteration = LocalDateTime.now(),
            created = LocalDateTime.now()
        )
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
        onView(isAssignableFrom(android.widget.AutoCompleteTextView::class.java)).perform(typeText("Manga"))
        
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

        // Stub da intent para evitar que a activity real inicie e acesse o DB fechado
        intending(hasComponent(MangaReaderActivity::class.java.name))
            .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))

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

        // Stub da intent para evitar que a activity real inicie e acesse o DB fechado
        intending(hasComponent(BookReaderActivity::class.java.name))
            .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))

        // Clica na anotação de livro (no root para evitar o clique na nota que abre o popup)
        onView(allOf(withId(R.id.book_annotation_root), hasDescendant(withText("Note for Book")))).perform(click())
        
        // Verifica se abriu a BookReaderActivity
        intended(allOf(
            hasComponent(BookReaderActivity::class.java.name),
            hasExtra(GeneralConsts.KEYS.BOOK.NAME, "Sample Book")
        ))
    }

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
        onView(withId(androidx.appcompat.R.id.alertTitle)).check(matches(withText(R.string.book_annotation_delete)))
        
        // Clica no botão de deletar (R.string.action_delete)
        onView(withId(android.R.id.button1)).perform(click())
        
        // Aguarda animação e atualização
        Thread.sleep(1000)
        
        // Verifica se a anotação sumiu
        onView(withText("Note for Manga")).check(doesNotExist())
    }

    @Test
    fun testHeaderNotSwipeable() {
        val scenario = ActivityScenario.launch(TestActivity::class.java)
        scenario.onActivity { activity ->
            activity.setFragment(AnnotationFragment())
        }

        Thread.sleep(2000)

        // Tenta fazer swipe em um cabeçalho (Sample Manga)
        onView(withText("Sample Manga")).perform(swipeLeft())

        // Verifica que o diálogo de confirmação NÃO apareceu (cabeçalhos não são deletáveis por swipe)
        onView(withId(androidx.appcompat.R.id.alertTitle)).check(doesNotExist())
    }

    @Test
    fun testCancelDelete() {
        val scenario = ActivityScenario.launch(TestActivity::class.java)
        scenario.onActivity { activity ->
            activity.setFragment(AnnotationFragment())
        }

        Thread.sleep(2000)

        // Inicia o swipe para deletar
        onView(withText("Note for Manga")).perform(swipeLeft())

        // Verifica se o diálogo apareceu
        onView(withId(androidx.appcompat.R.id.alertTitle)).check(matches(withText(R.string.book_annotation_delete)))

        // Clica em um botão fora (ou usa Voltar) para cancelar o diálogo
        // Ou simulamos o dismiss via clique fora se possível, mas aqui usaremos o "Voltar" ou se houver botão cancelar
        // Como o AlertDialog padrão tem botão negativo se definido (não parece ter no código original, mas o dismiss cancela)
        
        // Simula o clique fora ou cancelamento via sistema
        androidx.test.espresso.Espresso.pressBack()

        // Aguarda animação de retorno
        Thread.sleep(1000)

        // Verifica se a anotação AINDA EXISTE (foi restaurada pelo setOnDismissListener)
        onView(withText("Note for Manga")).check(matches(isDisplayed()))
    }

    @Test
    fun testScrollButtons() {
        val scenario = ActivityScenario.launch(TestActivity::class.java)
        scenario.onActivity { activity ->
            activity.setFragment(AnnotationFragment())
        }

        Thread.sleep(2000)

        // Inicialmente os botões devem estar invisíveis
        onView(withId(R.id.annotation_scroll_up)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.annotation_scroll_down)).check(matches(withEffectiveVisibility(Visibility.GONE)))

        // Para testar a visibilidade, precisaríamos de uma lista longa que permitisse scroll.
        // No setup atual temos poucas anotações.
    }
}
