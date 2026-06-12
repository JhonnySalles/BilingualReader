package br.com.fenix.bilingualreader.view.ui.detail.book

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.enums.Languages
import br.com.fenix.bilingualreader.service.parses.book.DocumentParse
import br.com.fenix.bilingualreader.service.parses.book.ImageParse
import br.com.fenix.bilingualreader.service.repository.BookRepository
import br.com.fenix.bilingualreader.service.repository.DataBase
import br.com.fenix.bilingualreader.util.secrets.Secrets
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class BookDetailViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var viewModel: BookDetailViewModel
    private lateinit var application: Application

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        application = ApplicationProvider.getApplicationContext()
        
        mockkConstructor(ImageParse::class)
        mockkConstructor(DocumentParse::class)
        
        mockkStatic(Dispatchers::class)
        every { Dispatchers.IO } returns testDispatcher
        every { Dispatchers.Default } returns testDispatcher
        
        mockkStatic("com.google.firebase.crashlytics.ktx.FirebaseCrashlyticsKt")
        val mockCrashlytics = mockk<FirebaseCrashlytics>(relaxed = true)
        every { Firebase.crashlytics } returns mockCrashlytics
        
        // Mock Secrets to avoid NPE
        mockkObject(Secrets.Instance)
        val mockSecrets = mockk<Secrets>(relaxed = true)
        every { Secrets.getSecrets(any()) } returns mockSecrets
        
        // COMPLETELY mock the Database to prevent any SQLite calls
        mockkObject(DataBase.Companion)
        val mockDb = mockk<DataBase>(relaxed = true)
        every { DataBase.getDataBase(any()) } returns mockDb
        
        mockkConstructor(BookRepository::class)
        every { anyConstructed<BookRepository>().get(any()) } returns Book(null, 1L, File("f.epub"))
        every { anyConstructed<BookRepository>().update(any(), any()) } just Runs
        every { anyConstructed<BookRepository>().update(any()) } just Runs
        every { anyConstructed<BookRepository>().delete(any()) } just Runs
        every { anyConstructed<BookRepository>().markRead(any()) } answers { 
            firstArg<Book?>()?.let { it.bookMark = it.pages }
        }
        every { anyConstructed<BookRepository>().clearHistory(any()) } just Runs
        every { anyConstructed<BookRepository>().findConfiguration(any()) } returns null
        
        viewModel = BookDetailViewModel(application)
    }

    @After
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    @Test
    fun `setBook should update liveData`() {
        val book = Book(null, 1L, File("test.epub"))
        viewModel.setBook(application, book)
        assertEquals("test", viewModel.book.value?.fileName)
    }

    @Test
    fun `delete should trigger repository deletion`() {
        val book = Book(null, 1L, File("test.epub"))
        viewModel.setBook(application, book)
        
        viewModel.delete()
        
        verify { anyConstructed<BookRepository>().delete(book) }
    }

    @Test
    fun `changeLanguage should update book and call repository`() {
        val book = Book(null, 1L, File("f.epub"))
        viewModel.setBook(application, book)
        
        viewModel.changeLanguage(Languages.PORTUGUESE)
        
        assertEquals(Languages.PORTUGUESE, viewModel.book.value?.language)
        verify { anyConstructed<BookRepository>().update(any(), any()) }
    }

    @Test
    fun `markRead should update book mark and notify`() {
        val book = Book(null, 1L, File("f.epub")).apply { pages = 100 }
        viewModel.setBook(application, book)
        
        viewModel.markRead()
        
        assertEquals(100, book.bookMark)
        verify { anyConstructed<BookRepository>().markRead(book) }
    }
}
