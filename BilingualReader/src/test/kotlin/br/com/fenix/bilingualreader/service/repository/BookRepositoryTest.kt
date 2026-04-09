package br.com.fenix.bilingualreader.service.repository

import android.content.Context
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.service.repository.BookDAO
import br.com.fenix.bilingualreader.service.repository.BookConfigurationDAO
import br.com.fenix.bilingualreader.service.repository.LibrariesDAO
import br.com.fenix.bilingualreader.service.repository.DataBase
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.mockk.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BookRepositoryTest {

    private val context = mockk<Context>()
    private val dataBase = mockk<DataBase>()
    private val bookDao = mockk<BookDAO>(relaxed = true)
    private val configDao = mockk<BookConfigurationDAO>(relaxed = true)
    private val librariesDao = mockk<LibrariesDAO>(relaxed = true)

    private lateinit var bookRepository: BookRepository

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        mockkStatic(FirebaseCrashlytics::class)
        every { FirebaseCrashlytics.getInstance() } returns mockk(relaxed = true)

        mockkObject(DataBase)
        every { DataBase.getDataBase(any()) } returns dataBase
        every { dataBase.getBookDao() } returns bookDao
        every { dataBase.getBookConfigurationDao() } returns configDao
        every { dataBase.getLibrariesDao() } returns librariesDao
        
        bookRepository = BookRepository(context)
    }

    @org.junit.After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `save should update lastAlteration and call DAO save`() {
        val book = mockk<Book>(relaxed = true)
        every { book.id } returns 1L
        every { (bookDao as DataBaseDAO<Book>).save(any<Book>()) } returns 1L
        
        val now = LocalDateTime.now()
        bookRepository.save(book, now)
        
        verify { book.lastAlteration = now }
        verify { bookDao.save(book) }
    }

    @Test
    fun `updateBookMark should set lastAlteration and call updateBookMark in DAO`() {
        val book = mockk<Book>(relaxed = true)
        every { book.id } returns 123L
        every { book.bookMark } returns 45
        
        bookRepository.updateBookMark(book)
        
        verify { book.lastAlteration = any() }
        verify { bookDao.updateBookMark(123L, 45) }
    }

    @Test
    fun `list should handle exceptions and log them`() {
        val library = Library(id = 1L)
        every { bookDao.list(1L) } throws RuntimeException("DB Error")
        
        // This should not crash despite DB Error
        val results = bookRepository.list(library)
        
        assertTrue("Should return an empty list on error", results.isEmpty())
    }

    @Test
    fun `loadLibrary in list should populate library object`() {
        val library = Library(id = 1L)
        val book = Book(null, "Title", "Author", "", "", null, "", "", "", "", 1, "", 0, "", 0, false, br.com.fenix.bilingualreader.model.enums.Languages.ENGLISH, "", "", "", br.com.fenix.bilingualreader.model.enums.FileType.UNKNOWN, 0L, false, 2L, mutableListOf(), false, null, null, null, java.util.Date(), null, null)
        val libraryEntity = Library(id = 2L, title = "Target Library")
        
        every { bookDao.list(1L) } returns listOf(book)
        every { librariesDao.get(2L) } returns libraryEntity
        
        val results = bookRepository.list(library)
        
        assertEquals(1, results.size)
        assertEquals("Target Library", results[0].library.title)
        verify { librariesDao.get(2L) }
    }
}
