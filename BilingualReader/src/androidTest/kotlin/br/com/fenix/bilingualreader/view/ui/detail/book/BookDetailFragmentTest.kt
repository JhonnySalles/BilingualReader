package br.com.fenix.bilingualreader.view.ui.detail.book

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
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.entity.Tags
import br.com.fenix.bilingualreader.model.enums.Languages
import br.com.fenix.bilingualreader.model.enums.Libraries
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.repository.DataBase
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.view.ui.detail.DetailActivity
import br.com.fenix.bilingualreader.view.ui.vocabulary.VocabularyActivity
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import androidx.test.espresso.ViewInteraction
import android.view.View
import br.com.fenix.bilingualreader.model.entity.History
import org.hamcrest.Matchers.anything
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.instanceOf
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
class BookDetailFragmentTest {

    private lateinit var db: DataBase
    private lateinit var mockBook: Book
    private lateinit var mockLib: Library

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

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        // Setup Database
        db = Room.inMemoryDatabaseBuilder(context, DataBase::class.java)
            .allowMainThreadQueries()
            .build()
        DataBase.setTestingInstance(db)

        // Setup Library
        val mockPath = File(context.cacheDir, "mock_books_detail")
        if (!mockPath.exists()) mockPath.mkdirs()
        
        mockLib = Library(
            id = 1L,
            title = "Test Library",
            path = mockPath.absolutePath,
            language = Libraries.ENGLISH,
            type = Type.BOOK
        )
        db.getLibrariesDao().save(mockLib)

        // Setup Book
        val bookFile = File(mockPath, "Detail Test Book.epub")
        if (!bookFile.exists()) bookFile.createNewFile()

        mockBook = Book(mockLib.id, 100L, bookFile).apply {
            title = "Detail Test Book"
            author = "Test Author"
            pages = 100
            bookMark = 50
            favorite = false
            language = Languages.PORTUGUESE
        }
        db.getBookDao().save(mockBook)

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
            putExtra(GeneralConsts.KEYS.OBJECT.BOOK, mockBook)
        }
    }

    @Test
    fun testBookDetailDisplay() {
        ActivityScenario.launch<DetailActivity>(getStartIntent())
        
        // Aguarda carregamento do ViewModel e possível sincronização inicial com o arquivo
        Thread.sleep(2000)
        waitForView(withId(R.id.book_detail_title))

        // Verifica os campos principais
        onView(withId(R.id.book_detail_title)).check(matches(withText(mockBook.title)))
        onView(withId(R.id.book_detail_author)).check(matches(withText(mockBook.author)))
        onView(withId(R.id.book_detail_folder)).check(matches(withText(mockBook.path)))
        
        // Verifica o progresso (texto)
        onView(withId(R.id.book_detail_book_mark)).check(matches(withText(containsString("50 / 100"))))
    }

    @Test
    fun testBookFullInformationDisplay() {
        // Popula o mock com dados detalhados
        mockBook.apply {
            publisher = "Editora Alpha"
            isbn = "978-1234567890"
            genre = "Fantasia, Aventura"
            volume = "Volume Unico"
            annotation = "Notas de teste detalhadas."
            author = "Escritor Teste"
            release = LocalDate.of(2025, 1, 1)
        }
        db.getBookDao().update(mockBook)

        val scenario = ActivityScenario.launch<DetailActivity>(getStartIntent())
        
        // Aguarda o app realizar a primeira sincronização (que sobrescreve os dados com vazio pelo arquivo estar vazio)
        Thread.sleep(2000)
        
        // Agora forçamos a atualização da UI injetando novamente o nosso mockBook no ViewModel
        scenario.onActivity { activity ->
            val viewModel = androidx.lifecycle.ViewModelProvider(activity).get(BookDetailViewModel::class.java)
            viewModel.setBook(activity, mockBook)
        }

        waitForView(withId(R.id.book_detail_information_publish))

        // Valida campos extraídos do Book na seção de informação
        onView(withId(R.id.book_detail_information_publish))
            .perform(scrollTo())
            .check(matches(withText(containsString("Editora Alpha"))))

        onView(withId(R.id.book_detail_information_isbn))
            .perform(scrollTo())
            .check(matches(withText(containsString("978-1234567890"))))

        onView(withId(R.id.book_detail_information_genres))
            .perform(scrollTo())
            .check(matches(withText(containsString("Fantasia, Aventura"))))

        onView(withId(R.id.book_detail_information_volume))
            .perform(scrollTo())
            .check(matches(withText(containsString("Volume Unico"))))

        onView(withId(R.id.book_detail_information_annotation))
            .perform(scrollTo())
            .check(matches(withText(containsString("Notas de teste detalhadas"))))

        onView(withId(R.id.book_detail_information_author))
            .perform(scrollTo())
            .check(matches(withText(containsString("Escritor Teste"))))

        onView(withId(R.id.book_detail_information_release))
            .perform(scrollTo())
            .check(matches(withText(containsString("2025"))))
    }

    @Test
    fun testFavoriteToggle() {
        ActivityScenario.launch<DetailActivity>(getStartIntent())
        
        Thread.sleep(2000)

        // Inicialmente não é favorito no DB
        assertFalse(db.getBookDao().get(mockBook.id!!)!!.favorite)

        // Clica no botão de favorito
        onView(withId(R.id.book_detail_button_favorite)).perform(click())
        
        // Aguarda persistência assíncrona do ViewModel
        Thread.sleep(500)
        
        // Verifica se o valor mudou no Banco de Dados
        assertTrue("O estado de favorito não foi persistido no Banco de Dados", 
            db.getBookDao().get(mockBook.id!!)!!.favorite)
            
        // Clica novamente para desmarcar
        onView(withId(R.id.book_detail_button_favorite)).perform(click())
        Thread.sleep(500)
        assertFalse("O estado de favorito deveria ter voltado para falso",
             db.getBookDao().get(mockBook.id!!)!!.favorite)
    }

    @Test
    fun testVocabularyNavigation() {
        ActivityScenario.launch<DetailActivity>(getStartIntent())
        Thread.sleep(2000)

        // Clica no botão de Vocabulário
        onView(withId(R.id.book_detail_button_vocabulary)).perform(scrollTo(), click())

        // Verifica se a intent para VocabularyActivity foi disparada
        intended(allOf(
            hasComponent(VocabularyActivity::class.java.name),
            hasExtra(GeneralConsts.KEYS.VOCABULARY.TYPE, Type.BOOK)
        ))
    }

    @Test
    fun testLanguageDropdownChange() {
        ActivityScenario.launch<DetailActivity>(getStartIntent())
        Thread.sleep(2000)

        // Clica no AutoCompleteTextView de idioma
        onView(withId(R.id.book_detail_information_menu_autocomplete_language)).perform(scrollTo(), click())

        // Aguarda a lista suspensa aparecer
        Thread.sleep(500)

        // Seleciona Inglês usando onData para maior estabilidade
        onData(anything())
            .inRoot(androidx.test.espresso.matcher.RootMatchers.isFocusable())
            .atPosition(1)
            .perform(click())

        Thread.sleep(1000)

        // Verifica se o idioma foi atualizado no DB
        val updatedBook = db.getBookDao().get(mockBook.id!!)!!
        assertEquals(Languages.ENGLISH, updatedBook.language)
    }

    @Test
    fun testTagsDisplay() {
        // Mock de uma Tag
        val mockTag = Tags(1L, "Fantasia", false)
        db.getTagsDao().save(mockTag)
        
        // Associa a tag ao livro
        mockBook.tags = mutableListOf(1L)
        db.getBookDao().update(mockBook)

        ActivityScenario.launch<DetailActivity>(getStartIntent())
        waitForView(withId(R.id.book_detail_information_tags_list))

        // Verifica se o nome da tag aparece na lista
        onView(withId(R.id.book_detail_information_tags_list))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
            
        onView(withText("Fantasia")).check(matches(isDisplayed()))
    }

    @Test
    fun testCoverPopup() {
        ActivityScenario.launch<DetailActivity>(getStartIntent())
        Thread.sleep(2000)

        // Clica na imagem de capa
        onView(withId(R.id.book_detail_book_image)).perform(click())

        // Verifica se o ImageView do popup subiu
        onView(withId(R.id.popup_detail_image))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
    }

    @Test
    fun testDeleteDialogAppearance() {
        ActivityScenario.launch<DetailActivity>(getStartIntent())
        
        Thread.sleep(2000)

        // Clica no botão de deletar
        onView(withId(R.id.book_detail_button_delete)).perform(scrollTo(), click())
        
        // Verifica se o diálogo de confirmação apareceu usando matcher específico para o título
        // para evitar choque com o botão 'book_detail_button_delete' que também tem o texto @string/book_detail_delete
        waitForView(allOf(withText(R.string.book_library_menu_delete), withId(androidx.appcompat.R.id.alertTitle)))
        
        // Cancela a ação
        onView(withText(R.string.action_negative)).perform(click())
        
        // Verifica se o diálogo sumiu
        Thread.sleep(500)
        onView(allOf(withText(R.string.book_library_menu_delete), withId(androidx.appcompat.R.id.alertTitle))).check(doesNotExist())
    }

    @Test
    fun testMarkReadButton() {
        ActivityScenario.launch<DetailActivity>(getStartIntent())
        Thread.sleep(2000)

        // Clica no botão de marcar como lido
        onView(withId(R.id.book_detail_button_mark_read)).perform(scrollTo(), click())
        
        Thread.sleep(500)
        
        val updatedBook = db.getBookDao().get(mockBook.id!!)!!
        assertEquals(updatedBook.pages, updatedBook.bookMark)
    }
}
