package br.com.fenix.bilingualreader.view.ui.book

import android.app.Application
import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.BookSearch
import br.com.fenix.bilingualreader.service.parses.book.DocumentParse
import br.com.fenix.bilingualreader.service.repository.BookSearchRepository
import br.com.fenix.bilingualreader.util.helpers.TextUtil
import br.com.fenix.bilingualreader.util.helpers.ThemeUtil
import br.com.fenix.bilingualreader.util.helpers.ThemeUtil.ThemeUtils.getColorFromAttr
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.ebookdroid.core.codec.CodecPage
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BookSearchViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var viewModel: BookSearchViewModel
    private lateinit var application: Application
    private val repository: BookSearchRepository = mockk(relaxed = true)
    private val documentParse: DocumentParse = mockk(relaxed = true)
    private val book: Book = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        
        application = ApplicationProvider.getApplicationContext()
        
        mockkObject(ThemeUtil.ThemeUtils)
        every { any<Context>().getColorFromAttr(any(), any(), any()) } returns 0xFF0000

        every { book.id } returns 1L
        every { book.fileName } returns "dummy"
        every { book.path } returns "dummy.epub"
        every { book.file } returns File("dummy.epub")
        every { book.folder } returns ""
        
        mockkConstructor(BookSearchRepository::class)
        every { anyConstructed<BookSearchRepository>().save(any()) } answers { repository.save(firstArg()) }
        every { anyConstructed<BookSearchRepository>().update(any()) } answers { repository.update(firstArg()) }
        every { anyConstructed<BookSearchRepository>().delete(any<Long>()) } answers { repository.delete(firstArg<Long>()) }
        every { anyConstructed<BookSearchRepository>().delete(any<BookSearch>()) } answers { repository.delete(firstArg<BookSearch>()) }
        every { anyConstructed<BookSearchRepository>().findAll(any()) } answers { repository.findAll(firstArg()) }
        
        // Ensure Dispatchers are globally mocked if needed, but runTest usually handles it.
        // However, the SearchViewModel uses CoroutineScope(Dispatchers.IO).launch
        mockkStatic(Dispatchers::class)
        every { Dispatchers.IO } returns testDispatcher

        mockkObject(TextUtil.TextUtils)
        every { TextUtil.TextUtils.formatHtml(any(), any()) } answers { firstArg() }
        every { TextUtil.TextUtils.highlightWordInText(any(), any(), any<Int>()) } answers { firstArg() }
        every { TextUtil.TextUtils.highlightWordInText(any(), any(), any<String>()) } answers { firstArg() }

        viewModel = BookSearchViewModel(application)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `initialize should load history from repository`() {
        val history = listOf(BookSearch(1L, 1L, "find", LocalDateTime.now()))
        every { repository.findAll(1L) } returns history
        
        viewModel.initialize(application, book, documentParse)
        
        assertEquals(history, viewModel.history.value)
    }

    @Test
    fun `save should update history list and call repository`() {
        viewModel.book = book
        val item = BookSearch(1L, "find")
        
        viewModel.save(item)
        
        assertTrue(viewModel.history.value!!.contains(item))
        verify { repository.save(item) }
    }

    @Test
    fun `deleteAll should clear history and call repository`() {
        val history = mutableListOf(BookSearch(1L, 1L, "find", LocalDateTime.now()))
        every { repository.findAll(1L) } returns history
        viewModel.initialize(application, book, documentParse)
        viewModel.deleteAll()
        
        assertEquals(0, viewModel.history.value!!.size)
        assertEquals(true, viewModel.history.value!!.isEmpty())
        verify { repository.delete(1L) }
    }

    @Test
    fun `search should find text in document pages`() = runTest {
        viewModel.initialize(application, book, documentParse)
        
        val page: CodecPage = mockk(relaxed = true)
        every { documentParse.getPage(any()) } returns page
        every { page.pageHTML } returns "This is a text to find something."
        every { documentParse.pageCount } returns 1
        every { documentParse.getChapters() } returns linkedMapOf("Chapter 1" to 0)

        // Mock Dispatchers.IO to use testDispatcher
        mockkStatic(Dispatchers::class)
        every { Dispatchers.IO } returns testDispatcher
        
        viewModel.search("find")
        
        val results = viewModel.search.value
        assertNotNull(results)
        // results may contain both HEADER and CONTENT
        assertTrue("Should have at least 1 result", results!!.size >= 1)
        assertTrue("Should contain 'find'", results.any { it.search.contains("find") })
    }
}
