package br.com.fenix.bilingualreader.view.ui.history

import android.app.Application
import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.model.interfaces.History
import br.com.fenix.bilingualreader.service.repository.BookRepository
import br.com.fenix.bilingualreader.service.repository.LibraryRepository
import br.com.fenix.bilingualreader.service.repository.MangaRepository
import br.com.fenix.bilingualreader.service.repository.TagsRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDateTime
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class HistoryViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var viewModel: HistoryViewModel
    private val application: Application = mockk(relaxed = true)
    private val context: Context = mockk(relaxed = true)
    
    private val mangaRepository: MangaRepository = mockk(relaxed = true)
    private val bookRepository: BookRepository = mockk(relaxed = true)
    private val libraryRepository: LibraryRepository = mockk(relaxed = true)
    private val tagsRepository: TagsRepository = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        
        every { application.applicationContext } returns context
        every { context.getString(any()) } returns "Default"

        mockkConstructor(MangaRepository::class)
        every { anyConstructed<MangaRepository>().listHistory() } answers { mangaRepository.listHistory() }
        every { anyConstructed<MangaRepository>().save(any()) } answers { mangaRepository.save(firstArg()) }
        every { anyConstructed<MangaRepository>().update(any()) } answers { mangaRepository.update(firstArg()) }
        
        mockkConstructor(BookRepository::class)
        every { anyConstructed<BookRepository>().listHistory() } answers { bookRepository.listHistory() }
        every { anyConstructed<BookRepository>().save(any()) } answers { bookRepository.save(firstArg()) }
        every { anyConstructed<BookRepository>().update(any()) } answers { bookRepository.update(firstArg()) }

        mockkConstructor(LibraryRepository::class)
        mockkConstructor(TagsRepository::class)
        every { anyConstructed<TagsRepository>().list() } returns arrayListOf()

        viewModel = HistoryViewModel(application)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `list should fetch from both repositories and sort by last access`() = runTest {
        val now = LocalDateTime.now()
        val book = Book(null, null, File("Book1")).apply { lastAccess = now.minusHours(1) }
        val manga = Manga(null, null, File("Manga1")).apply { lastAccess = now }
        
        every { bookRepository.listHistory() } returns mutableListOf(book)
        every { mangaRepository.listHistory() } returns mutableListOf(manga)
        
        mockkStatic(Dispatchers::class)
        every { Dispatchers.IO } returns testDispatcher

        viewModel.list()
        
        val history = viewModel.history.value
        assertNotNull(history)
        assertEquals(2, history!!.size)
        // Manga should be first (more recent)
        assertEquals("Manga1", history[0].name)
        assertEquals("Book1", history[1].name)
    }

    @Test
    fun `filterType should filter history by manga or book`() {
        val book = Book(null, null, File("Book1")).apply { lastAccess = LocalDateTime.now() }
        val manga = Manga(null, null, File("Manga1")).apply { lastAccess = LocalDateTime.now() }
        
        viewModel.update(listOf(book, manga))
        
        viewModel.filterType(Type.MANGA)
        assertEquals(1, viewModel.history.value!!.size)
        assertTrue(viewModel.history.value!![0] is Manga)
        
        viewModel.filterType(Type.BOOK)
        assertEquals(1, viewModel.history.value!!.size)
        assertTrue(viewModel.history.value!![0] is Book)
    }

    @Test
    fun `save should call appropriate repository based on entity type`() {
        val book = Book(null, null, File("Book"))
        val manga = Manga(null, null, File("Manga"))
        
        viewModel.save(book)
        verify { bookRepository.save(book) }
        
        viewModel.save(manga)
        verify { mangaRepository.save(manga) }
    }
}
