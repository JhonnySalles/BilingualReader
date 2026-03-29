package br.com.fenix.bilingualreader.service.scanner

import android.content.Context
import br.com.ebook.foobnix.entity.FileMeta
import br.com.ebook.foobnix.entity.FileMetaCore
import br.com.ebook.foobnix.ext.EbookMeta
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.service.repository.Storage
import br.com.fenix.bilingualreader.util.helpers.Notifications
import io.mockk.*
import org.junit.*
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ScannerBookTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var library: Library

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        
        // Mock static Notification helpers
        mockkObject(Notifications.NotificationUtils)
        every { Notifications.getNotification(any(), any(), any()) } returns mockk(relaxed = true)
        every { Notifications.getID() } returns 1
        
        // Mock FileMetaCore (Singleton in Java)
        mockkStatic(FileMetaCore::class)
        val fileMetaCore = mockk<FileMetaCore>(relaxed = true)
        every { FileMetaCore.get() } returns fileMetaCore
        every { fileMetaCore.getEbookMeta(any(), any(), any()) } returns EbookMeta.Empty()
        
        // Mock Storage constructor
        mockkConstructor(Storage::class)
        every { anyConstructed<Storage>().listBook(any()) } returns emptyList()
        every { anyConstructed<Storage>().listBookDeleted(any()) } returns emptyList()
        every { anyConstructed<Storage>().findBookByPath(any()) } returns null
        every { anyConstructed<Storage>().save(book = any<Book>(), lastAlteration = any()) } returns 1L

        library = Library(id = 2L, title = "Book Library", path = tempFolder.root.absolutePath)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun libraryUpdateRunnable_detectsAndSavesNewBook() {
        // Create a fake book file
        val bookFile = tempFolder.newFile("book_test.epub")
        
        val scanner = ScannerBook(context)
        
        // Access private inner class using reflection
        val runnableClass = ScannerBook::class.java.declaredClasses.find { it.name.contains("LibraryUpdateRunnable") }
        val constructor = runnableClass?.getDeclaredConstructor(ScannerBook::class.java, UUID::class.java, Library::class.java, Boolean::class.java)
        constructor?.isAccessible = true
        val runnable = constructor?.newInstance(scanner, UUID.randomUUID(), library, true) as Runnable
        
        runnable.run()
        
        // Verify that storage.save was called for the new book
        verify(atLeast = 1) { 
            anyConstructed<Storage>().save(book = match { it.path == bookFile.absolutePath }, lastAlteration = any()) 
        }
    }

    @Test
    fun libraryUpdateRunnable_deletesMissingBook() {
        val missingBook = mockk<Book>(relaxed = true)
        every { missingBook.path } returns "/non/existent/book.pdf"
        every { anyConstructed<Storage>().listBook(any()) } returns listOf(missingBook)
        
        val scanner = ScannerBook(context)
        
        val runnableClass = ScannerBook::class.java.declaredClasses.find { it.name.contains("LibraryUpdateRunnable") }
        val constructor = runnableClass?.getDeclaredConstructor(ScannerBook::class.java, UUID::class.java, Library::class.java, Boolean::class.java)
        constructor?.isAccessible = true
        val runnable = constructor?.newInstance(scanner, UUID.randomUUID(), library, true) as Runnable
        
        runnable.run()
        
        verify { anyConstructed<Storage>().delete(missingBook) }
    }
}
