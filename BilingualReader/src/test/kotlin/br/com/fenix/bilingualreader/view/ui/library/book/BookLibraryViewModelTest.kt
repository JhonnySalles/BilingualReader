package br.com.fenix.bilingualreader.view.ui.library.book

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.enums.Order
import br.com.fenix.bilingualreader.service.repository.BookRepository
import br.com.fenix.bilingualreader.service.repository.TagsRepository
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.Util
import java.io.File
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import br.com.fenix.bilingualreader.model.enums.Filter as FilterType

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BookLibraryViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var viewModel: BookLibraryViewModel
    private val application: Application = mockk(relaxed = true)
    private val context: Context = mockk(relaxed = true)
    private val sharedPreferences: SharedPreferences = mockk(relaxed = true)
    private val bookRepository: BookRepository = mockk(relaxed = true)
    private val tagsRepository: TagsRepository = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        
        mockkObject(GeneralConsts.Companion)
        every { GeneralConsts.getSharedPreferences(any()) } returns sharedPreferences

        every { application.applicationContext } returns context
        
        mockkConstructor(BookRepository::class)
        every { anyConstructed<BookRepository>().list(any()) } answers { bookRepository.list(firstArg()) }
        every { anyConstructed<BookRepository>().save(any()) } answers { bookRepository.save(firstArg()) }
        every { anyConstructed<BookRepository>().update(any()) } answers { bookRepository.update(firstArg()) }
        every { anyConstructed<BookRepository>().delete(any()) } answers { bookRepository.delete(firstArg()) }

        mockkConstructor(TagsRepository::class)
        every { anyConstructed<TagsRepository>().list() } answers { tagsRepository.list() }

        viewModel = BookLibraryViewModel(application)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `list should fetch books from repository and update LiveData`() = runTest {
        val library = Library(null, "MyLibrary").apply { id = 1L }
        val books = mutableListOf(Book(library.id!!, 1L, File("Path 1")), Book(library.id!!, 2L, File("Path 2")))
        every { bookRepository.list(any()) } returns books

        viewModel.setLibrary(library)
        
        // Mock Dispatchers.IO
        mockkStatic(Dispatchers::class)
        every { Dispatchers.IO } returns testDispatcher
        
        var callbackCalled = false
        viewModel.list { success ->
            callbackCalled = true
            assertTrue(success)
        }

        assertTrue(callbackCalled)
        assertEquals(2, viewModel.listBook.value!!.size)
        assertEquals("Book 1", viewModel.listBook.value!![0].name)
        assertFalse(viewModel.loading.value!!)
    }

    @Test
    fun `sorted(Name) should sort books alphabetically`() {
        val bookA = Book(1L, 1L, File("P1")).apply { name = "Apple" }
        val bookB = Book(1L, 2L, File("P2")).apply { name = "Banana" }
        val bookC = Book(1L, 3L, File("P3")).apply { name = "Cherry" }
        
        viewModel.setList(arrayListOf(bookB, bookC, bookA))
        
        viewModel.sorted(Order.Name, isDesc = false)
        
        assertEquals("Apple", viewModel.listBook.value!![0].name)
        assertEquals("Banana", viewModel.listBook.value!![1].name)
        assertEquals("Cherry", viewModel.listBook.value!![2].name)
    }

    @Test
    fun `sorted(Name, isDesc=true) should sort books in reverse alphabetically`() {
        val bookA = Book(1L, 1L, File("P1")).apply { name = "Apple" }
        val bookB = Book(1L, 2L, File("P2")).apply { name = "Banana" }
        
        viewModel.setList(arrayListOf(bookA, bookB))
        
        viewModel.sorted(Order.Name, isDesc = true)
        
        assertEquals("Banana", viewModel.listBook.value!![0].name)
        assertEquals("Apple", viewModel.listBook.value!![1].name)
    }

    @Test
    fun `filter search by name should narrow the list`() {
        val book1 = Book(1L, 1L, File("P1")).apply { name = "Harry Potter" }
        val book2 = Book(1L, 2L, File("P2")).apply { name = "Lord of the Rings" }
        viewModel.setList(arrayListOf(book1, book2))

        viewModel.getFilter().filter("harry")
        
        val filtered = viewModel.listBook.value
        assertEquals(1, filtered!!.size)
        assertEquals("Harry Potter", filtered[0].name)
    }

    @Test
    fun `advanced filter with prefix should work`() {
        val book1 = Book(1L, 1L, File("P1")).apply { name = "B1"; author = "J.K. Rowling" }
        val book2 = Book(1L, 2L, File("P2")).apply { name = "B2"; author = "Tolkien" }
        viewModel.setList(arrayListOf(book1, book2))

        // We need to mock Util.stringToFilter because it used inside the filter implementation
        mockkStatic(Util::class)
        every { Util.stringToFilter(any(), any(), any()) } answers {
            val prefix = thirdArg<String>()
            if (prefix.equals("Author", ignoreCase = true)) FilterType.Author else FilterType.None
        }

        viewModel.getFilter().filter("@Author:Rowling")
        
        val filtered = viewModel.listBook.value
        assertEquals(1, filtered!!.size)
        assertEquals("J.K. Rowling", filtered[0].author)
    }

    @Test
    fun `stack management should allow pushing and popping libraries`() {
        val library1 = Library(null, "Lib 1").apply { id = 1L }
        val library2 = Library(null, "Lib 2").apply { id = 2L }
        val book1 = Book(1L, 1L, File("P1")).apply { name = "Book 1" }
        
        viewModel.setLibrary(library1)
        viewModel.setList(arrayListOf(book1))
        
        viewModel.addStackLibrary("stack1", library1)
        
        viewModel.setLibrary(library2)
        viewModel.setList(arrayListOf())
        
        assertEquals(2L, viewModel.getLibrary().id)
        assertTrue(viewModel.listBook.value!!.isEmpty())
        
        viewModel.restoreLastStackLibrary("stack1")
        
        assertEquals(1L, viewModel.getLibrary().id)
        assertEquals(1, viewModel.listBook.value!!.size)
        assertEquals("Book 1", viewModel.listBook.value!![0].name)
    }
}
