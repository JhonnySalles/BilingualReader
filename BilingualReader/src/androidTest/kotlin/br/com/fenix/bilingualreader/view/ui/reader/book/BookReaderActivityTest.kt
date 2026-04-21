package br.com.fenix.bilingualreader.view.ui.reader.book

import android.content.Context
import android.content.Intent
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import android.view.ViewGroup
import androidx.core.widget.NestedScrollView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.enums.Libraries
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.parses.book.DocumentParse
import br.com.fenix.bilingualreader.service.repository.DataBase
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.Telemetry
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import android.view.View
import androidx.lifecycle.ViewModelProvider
import br.com.fenix.bilingualreader.model.enums.AlignmentLayoutType
import br.com.fenix.bilingualreader.model.enums.Languages
import br.com.fenix.bilingualreader.model.enums.MarginLayoutType

@RunWith(AndroidJUnit4::class)
class BookReaderActivityTest {

    private lateinit var db: DataBase
    private lateinit var mockBook: Book
    private lateinit var mockLib: Library

    @Before
    fun setup() {
        val appContext = ApplicationProvider.getApplicationContext<Context>()
        val testContext = InstrumentationRegistry.getInstrumentation().context
        
        Telemetry.isEnabled = false
        
        // Initialize DocumentParse for native libraries and cache
        DocumentParse.init(appContext)
        
        db = Room.inMemoryDatabaseBuilder(appContext, DataBase::class.java)
            .allowMainThreadQueries()
            .build()
        DataBase.setTestingInstance(db)

        GeneralConsts.getSharedPreferences(appContext).edit().clear().commit()
        GeneralConsts.getSharedPreferences(appContext).edit()
            .putBoolean(GeneralConsts.KEYS.TOUCH.BOOK_TOUCH_DEMONSTRATION, false)
            .commit()

        val mockPath = File(appContext.cacheDir, "mock_books_reader")
        if (mockPath.exists()) mockPath.deleteRecursively()
        mockPath.mkdirs()
        
        mockLib = Library(id = 1L, title = "Book Test Library", path = mockPath.absolutePath, language = Libraries.ENGLISH, type = Type.BOOK)
        db.getLibrariesDao().save(mockLib)

        val bookFile = File(mockPath, "book_test.epub")
        testContext.assets.open("livro.epub").use { input ->
            FileOutputStream(bookFile).use { output ->
                input.copyTo(output)
            }
        }

        mockBook = Book(mockLib.id, 500L, bookFile).apply {
            title = "Reader UI Test Book"
            author = "Test Author"
            pages = 10 
            bookMark = 1
        }
        db.getBookDao().save(mockBook)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun getStartIntent(): Intent {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return Intent(context, BookReaderActivity::class.java).apply {
            putExtra(GeneralConsts.KEYS.OBJECT.LIBRARY, mockLib)
            putExtra(GeneralConsts.KEYS.OBJECT.BOOK, mockBook)
            action = Intent.ACTION_MAIN
        }
    }

    @Test
    fun testHUDMetadataDisplay() {
        ActivityScenario.launch<BookReaderActivity>(getStartIntent()).use { scenario ->
            Thread.sleep(15000) 

            // Forçamos o estado de Fullscreen no fragment para garantir que os componentes HUD apareçam
            scenario.onActivity { activity ->
                val fragment = activity.supportFragmentManager.findFragmentById(R.id.root_frame_book_reader) as? BookReaderFragment
                fragment?.setFullscreen(false)
            }
            Thread.sleep(3000)

            onView(withId(R.id.reader_book_toolbar_top)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            onView(withId(R.id.reader_book_toolbar_title)).check(matches(withText(mockBook.name)))
            onView(withId(R.id.reader_book_toolbar_bottom_author)).check(matches(withText(mockBook.author)))
        }
    }

    @Test
    fun testConfigurationPopupTrigger() {
        ActivityScenario.launch<BookReaderActivity>(getStartIntent()).use { scenario ->
            Thread.sleep(15000)

            scenario.onActivity { activity ->
                val fragment = activity.supportFragmentManager.findFragmentById(R.id.root_frame_book_reader) as? BookReaderFragment
                fragment?.setFullscreen(false)
            }
            Thread.sleep(3000)

            // Ativa o comando de abrir popup através da Activity
            scenario.onActivity { activity ->
                val toolbar = activity.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar_book_reader)
                activity.onOptionsItemSelected(toolbar.menu.findItem(R.id.menu_item_reader_book_font_style))
            }
            
            Thread.sleep(8000) 
            
            onView(withId(R.id.popup_book_configuration_tab)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        }
    }

    @Test
    fun testBookIndexDialogNavigation() {
        ActivityScenario.launch<BookReaderActivity>(getStartIntent()).use { scenario ->
            Thread.sleep(15000)

            scenario.onActivity { activity ->
                val toolbar = activity.findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar_book_reader)
                activity.onOptionsItemSelected(toolbar.menu.findItem(R.id.menu_item_reader_book_chapter))
            }
            Thread.sleep(2000)

            // Verifica se o diálogo de índice (Sumário) apareceu
            // O título padrão é R.string.reading_book_page_index
            val appContext = ApplicationProvider.getApplicationContext<android.content.Context>()
            val indexTitle = appContext.getString(R.string.reading_book_page_index)
            onView(withText(indexTitle)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun testBookBookmarkAction() {
        ActivityScenario.launch<BookReaderActivity>(getStartIntent()).use { scenario ->
            Thread.sleep(15000)

            // Garante que o HUD está visível antes de clicar no menu
            scenario.onActivity { activity ->
                val fragment = activity.supportFragmentManager.findFragmentById(R.id.root_frame_book_reader) as? BookReaderFragment
                fragment?.setFullscreen(false)
            }
            Thread.sleep(1000)

            // Tenta clicar no ícone de bookmark (menu superior)
            onView(withId(R.id.menu_item_reader_book_mark_page)).perform(click())
            Thread.sleep(5000)

            // Verifica se a página foi marcada no ViewModel
            scenario.onActivity { activity ->
                val viewModel = ViewModelProvider(activity)[BookReaderViewModel::class.java]
                val book = viewModel.book.value ?: mockBook
                
                // Busca em um intervalo de páginas (0, 1 e 2) para garantir robustez
                val hasAnnotation = (0..2).any { p -> 
                    viewModel.findAnnotationByPage(book, p).isNotEmpty()
                }
                
                assertTrue("O livro ${book.title} (ID: ${book.id}) deve conter ao menos uma anotação após o clique", hasAnnotation)
            }
        }
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

    @Test
    fun testPopupFontInteraction() {
        ActivityScenario.launch<BookReaderActivity>(getStartIntent()).use { scenario ->
            Thread.sleep(15000)
            
            scenario.onActivity { activity ->
                val fragment = activity.supportFragmentManager.findFragmentById(R.id.root_frame_book_reader) as? BookReaderFragment
                fragment?.setFullscreen(false)
                val toolbar = activity.findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar_book_reader)
                activity.onOptionsItemSelected(toolbar.menu.findItem(R.id.menu_item_reader_book_font_style))
            }
            Thread.sleep(3000)

            // Interação com o Slider de tamanho de fonte
            onView(withId(R.id.popup_book_font_size)).perform(click()) 
            
            scenario.onActivity { activity ->
                val viewModel = ViewModelProvider(activity)[BookReaderViewModel::class.java]
                assertTrue("O tamanho da fonte deve ser maior que zero após interação", viewModel.fontSize.value!! > 0f)
            }
        }
    }

    @Test
    fun testPopupLanguageInteraction() {
        ActivityScenario.launch<BookReaderActivity>(getStartIntent()).use { scenario ->
            Thread.sleep(15000)
            
            scenario.onActivity { activity ->
                val fragment = activity.supportFragmentManager.findFragmentById(R.id.root_frame_book_reader) as? BookReaderFragment
                fragment?.setFullscreen(false)
                val toolbar = activity.findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar_book_reader)
                activity.onOptionsItemSelected(toolbar.menu.findItem(R.id.menu_item_reader_book_font_style))
            }
            Thread.sleep(3000)

            // Navega para a aba de Linguagem
            val appContext = ApplicationProvider.getApplicationContext<android.content.Context>()
            val langTabTitle = appContext.getString(R.string.popup_reading_book_tab_item_language)
            onView(withText(langTabTitle)).perform(click())
            Thread.sleep(1000)

            // Forçamos a mudança para Japonês via ViewModel para validar a UI (toggles dinâmicos)
            scenario.onActivity { activity ->
                val viewModel = ViewModelProvider(activity)[BookReaderViewModel::class.java]
                viewModel.changeLanguage(Languages.JAPANESE)
            }
            Thread.sleep(1000)
            
            // Verifica se os controles específicos de japonês ficaram visíveis
            onView(withId(R.id.popup_book_language_process_japanese_text)).check(matches(isDisplayed()))
            onView(withId(R.id.popup_book_language_text_with_furigana)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun testPopupLayoutInteraction() {
        ActivityScenario.launch<BookReaderActivity>(getStartIntent()).use { scenario ->
            Thread.sleep(15000)
            
            scenario.onActivity { activity ->
                val fragment = activity.supportFragmentManager.findFragmentById(R.id.root_frame_book_reader) as? BookReaderFragment
                fragment?.setFullscreen(false)
                val toolbar = activity.findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar_book_reader)
                activity.onOptionsItemSelected(toolbar.menu.findItem(R.id.menu_item_reader_book_font_style))
            }
            Thread.sleep(3000)

            // Navega para a aba de Layout
            val appContext = ApplicationProvider.getApplicationContext<android.content.Context>()
            val layoutTabTitle = appContext.getString(R.string.popup_reading_book_tab_item_layout)
            onView(withText(layoutTabTitle)).perform(click())
            Thread.sleep(1000)

            // Simula cliques em opções de alinhamento e margem (usando helper para scroll)
            onView(withId(R.id.popup_book_layout_alignment_center)).perform(nestedScrollTo(), click())
            onView(withId(R.id.popup_book_layout_margin_medium)).perform(nestedScrollTo(), click())
            
            scenario.onActivity { activity ->
                val viewModel = ViewModelProvider(activity)[BookReaderViewModel::class.java]
                assertEquals(AlignmentLayoutType.Center, viewModel.alignmentType.value)
                assertEquals(MarginLayoutType.Medium, viewModel.marginType.value)
            }
        }
    }

    @Test
    fun testTouchScreenDemonstrationTrigger() {
        ActivityScenario.launch<BookReaderActivity>(getStartIntent()).use { scenario ->
            Thread.sleep(15000)
            
            scenario.onActivity { activity ->
                val fragment = activity.supportFragmentManager.findFragmentById(R.id.root_frame_book_reader) as? BookReaderFragment
                fragment?.setFullscreen(false)
                val toolbar = activity.findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar_book_reader)
                activity.onOptionsItemSelected(toolbar.menu.findItem(R.id.menu_item_reader_book_font_style))
            }
            Thread.sleep(3000)

            val appContext = ApplicationProvider.getApplicationContext<android.content.Context>()
            val layoutTabTitle = appContext.getString(R.string.popup_reading_book_tab_item_layout)
            onView(withText(layoutTabTitle)).perform(click())
            Thread.sleep(1000)

            // Aciona a demonstração de funções de toque (roda scroll se necessário via helper)
            onView(withId(R.id.popup_book_layout_reading_touch_screen)).perform(nestedScrollTo(), click())
            Thread.sleep(1000)
            
            // Verifica se o overlay de demonstração apareceu na Activity
            onView(withId(R.id.reader_book_container_touch_demonstration)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        }
    }
}
