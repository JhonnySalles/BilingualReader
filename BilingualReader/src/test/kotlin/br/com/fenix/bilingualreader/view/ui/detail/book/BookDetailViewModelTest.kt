package br.com.fenix.bilingualreader.view.ui.detail.book

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.enums.Languages
import br.com.fenix.bilingualreader.service.repository.BookRepository
import br.com.fenix.bilingualreader.service.repository.FileLinkRepository
import br.com.fenix.bilingualreader.service.repository.TagsRepository
import io.mockk.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import br.com.fenix.bilingualreader.util.secrets.Secrets
import br.com.ebook.foobnix.entity.FileMetaCore
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BookDetailViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: BookDetailViewModel
    private lateinit var application: Application
    private val bookRepository: BookRepository = mockk(relaxed = true)
    private val tagsRepository: TagsRepository = mockk(relaxed = true)

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        mockkConstructor(BookRepository::class)
        mockkConstructor(FileLinkRepository::class)
        mockkConstructor(TagsRepository::class)
        
        mockkObject(Secrets.Instance)
        every { Secrets.getSecrets(any()) } returns mockk(relaxed = true)
        
        mockkStatic(FileMetaCore::class)
        every { FileMetaCore.get() } returns mockk(relaxed = true)
        
        every { anyConstructed<BookRepository>().get(any()) } returns Book(null, 1L, File("f.epub"))
        every { anyConstructed<BookRepository>().update(any()) } just Runs
        every { anyConstructed<BookRepository>().delete(any()) } just Runs
        every { anyConstructed<BookRepository>().markRead(any()) } just Runs
        every { anyConstructed<BookRepository>().clearHistory(any()) } just Runs
        every { anyConstructed<TagsRepository>().list() } returns mutableListOf()
        every { anyConstructed<FileLinkRepository>().findAllByManga(any()) } returns mutableListOf()
        
        viewModel = BookDetailViewModel(application)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `setBook should update liveData`() {
        val book = Book(null, 10L, File("novel.epub"))
        viewModel.setBook(application, book)
        
        assertEquals(book, viewModel.book.value)
    }

    @Test
    fun `changeLanguage should update book and call repository`() {
        val book = Book(null, 10L, File("novel.epub"))
        viewModel.setBook(application, book)
        
        viewModel.changeLanguage(Languages.ENGLISH)
        assertEquals(Languages.ENGLISH, viewModel.book.value?.language)
        verify { anyConstructed<BookRepository>().update(any()) }
    }

    @Test
    fun `delete should trigger repository deletion`() {
        val book = Book(null, 10L, File("novel.epub"))
        viewModel.setBook(application, book)
        
        viewModel.delete()
        verify { anyConstructed<BookRepository>().delete(any()) }
    }

    @Test
    fun `markRead and clearHistory should interact with repository`() {
        val book = Book(null, 10L, File("novel.epub"))
        viewModel.setBook(application, book)
        
        viewModel.markRead()
        verify { anyConstructed<BookRepository>().markRead(any()) }
        
        viewModel.clearHistory()
        verify { anyConstructed<BookRepository>().clearHistory(any()) }
    }
}
