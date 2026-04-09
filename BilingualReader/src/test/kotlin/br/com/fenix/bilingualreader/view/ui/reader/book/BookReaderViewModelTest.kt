package br.com.fenix.bilingualreader.view.ui.reader.book

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.BookConfiguration
import br.com.fenix.bilingualreader.model.enums.*
import br.com.fenix.bilingualreader.service.repository.BookRepository
import br.com.fenix.bilingualreader.service.repository.VocabularyRepository
import br.com.fenix.bilingualreader.service.repository.BookAnnotationRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BookReaderViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var viewModel: BookReaderViewModel
    private lateinit var application: Application
    private val bookRepository: BookRepository = mockk(relaxed = true)
    private val vocabularyRepository: VocabularyRepository = mockk(relaxed = true)
    private val annotationRepository: BookAnnotationRepository = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        application = ApplicationProvider.getApplicationContext()

        mockkConstructor(BookRepository::class)
        every { anyConstructed<BookRepository>().findConfiguration(any()) } answers { bookRepository.findConfiguration(firstArg()) }
        every { anyConstructed<BookRepository>().saveConfiguration(any()) } answers { bookRepository.saveConfiguration(firstArg()) }
        every { anyConstructed<BookRepository>().updateConfiguration(any()) } answers { bookRepository.updateConfiguration(firstArg()) }

        mockkConstructor(VocabularyRepository::class)
        mockkConstructor(BookAnnotationRepository::class)

        viewModel = BookReaderViewModel(application)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `changeLanguage should update liveData and trigger font reload if needed`() {
        viewModel.changeLanguage(Languages.JAPANESE, true)
        assertEquals(Languages.JAPANESE, viewModel.language.value)
        assertEquals(true, viewModel.fontType.value?.isJapanese() ?: false)
    }

    @Test
    fun `changeFontSize should update liveData and regenerate CSS`() {
        viewModel.changeFontSize(20f)
        assertEquals(20f, viewModel.fontSize.value)
        assertNotNull(viewModel.fontCss.value)
    }

    @Test
    fun `changeScrolling should update liveData and save configuration`() {
        val config = BookConfiguration(10L, 1L, AlignmentLayoutType.Justify, MarginLayoutType.Small, SpacingLayoutType.Small, FontType.TimesNewRoman, 16f, ScrollingType.Pagination, PaginationType.Default)
        viewModel.loadConfiguration(config)
        
        viewModel.changeScrolling(ScrollingType.Scrolling)
        assertEquals(ScrollingType.Scrolling, viewModel.scrollingMode.value)
        verify { bookRepository.updateConfiguration(any()) }
    }

    @Test
    fun `loadConfiguration should initialize all properties from config entity`() {
        val config = BookConfiguration(
            id = 10L,
            idBook = 1L,
            alignment = AlignmentLayoutType.Center,
            margin = MarginLayoutType.Big,
            spacing = SpacingLayoutType.Big,
            fontType = FontType.BabelStoneErjian1,
            fontSize = 24f,
            scrolling = ScrollingType.Scrolling,
            pagination = PaginationType.Default
        )
        
        viewModel.loadConfiguration(config)
        
        assertEquals(AlignmentLayoutType.Center, viewModel.alignmentType.value)
        assertEquals(MarginLayoutType.Big, viewModel.marginType.value)
        assertEquals(SpacingLayoutType.Big, viewModel.spacingType.value)
        assertEquals(FontType.BabelStoneErjian1, viewModel.fontType.value)
        assertEquals(24f, viewModel.fontSize.value)
        assertEquals(ScrollingType.Scrolling, viewModel.scrollingMode.value)
        assertEquals(PaginationType.Default, viewModel.paginationType.value)
    }
}
