package br.com.fenix.bilingualreader.service.scanner

import android.content.Context
import br.com.ebook.foobnix.entity.FileMetaCore
import br.com.ebook.foobnix.ext.EbookMeta
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.service.repository.Storage
import br.com.fenix.bilingualreader.util.helpers.Notifications
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ScannerBookTest {

    @Rule @JvmField
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var library: Library

    @Before
    fun setUp() {
        context = androidx.test.core.app.ApplicationProvider.getApplicationContext<Context>()
        
        // Initialize CacheZipUtils with a temporary directory
        br.com.ebook.foobnix.ext.CacheZipUtils.init(context, tempFolder.newFolder("scanner_tests"))

        // Mock static Notification helpers
        mockkObject(Notifications.NotificationUtils)
        every { Notifications.getNotification(any(), any(), any()) } returns mockk(relaxed = true)
        every { Notifications.getID() } returns 1
        
        // Mock FileMetaCore (Singleton in Java)
        mockkStatic(FileMetaCore::class)
        val fileMetaCore = mockk<FileMetaCore>(relaxed = true)
        every { FileMetaCore.get() } returns fileMetaCore
        every { fileMetaCore.getEbookMeta(any(), any(), any()) } returns EbookMeta.Empty()
        
        // Mock Firebase
        mockkStatic(FirebaseApp::class)
        every { FirebaseApp.initializeApp(any()) } returns mockk()
        mockkStatic(FirebaseCrashlytics::class)
        every { FirebaseCrashlytics.getInstance() } returns mockk(relaxed = true)

        // Mock Storage constructor
        mockkConstructor(Storage::class)
        every { anyConstructed<Storage>().listBook(any()) } returns emptyList()
        every { anyConstructed<Storage>().listBookDeleted(any()) } returns emptyList()
        every { anyConstructed<Storage>().findBookByPath(any()) } returns null
        every { anyConstructed<Storage>().save(book = any<Book>(), lastAlteration = any()) } returns 1L

        val tempDirFile = tempFolder.root
        library = Library(id = 2L, title = "Book Library", path = tempDirFile.absolutePath)
        // Ensure the directory is not empty so 'walked' flag is set to true in ScannerBook
        File(tempDirFile, "placeholder.txt").createNewFile()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    /*@Test // Depurar e ajustar, pois a IA não consegue resolver.
    fun libraryUpdateRunnable_detectsAndSavesNewBook() {
        // Create a fake book file
        val bookFile = tempFolder.newFile("book_test.epub")
        
        val scanner = ScannerBook(context)
        
        // Access private inner class using reflection
        val runnableClass = ScannerBook::class.java.declaredClasses.find { it.name.contains("LibraryUpdateRunnable") }
        val constructor = runnableClass?.getDeclaredConstructor(ScannerBook::class.java, UUID::class.java, Library::class.java, Boolean::class.java)
        constructor?.isAccessible = true
        val runnableObj = constructor?.newInstance(scanner, UUID.randomUUID(), library, true)
        
        // Use reflection to call run() to avoid ClassCastException
        val runMethod = runnableClass?.getMethod("run")
        runMethod?.invoke(runnableObj)
        
        // Verify the code reached the scan loop
        verify(atLeast = 1) { anyConstructed<Storage>().listBook(any()) }

        // Verify that storage.save was called for the new book
        verify(atLeast = 1) { anyConstructed<Storage>().save(book = any<Book>(), lastAlteration = any()) }
        verify(atLeast = 1) { 
            anyConstructed<Storage>().save(book = match<Book> { 
                it.path.contains("book_test.epub", ignoreCase = true) 
            }, lastAlteration = any()) 
        }
    }*/

    @Test
    fun libraryUpdateRunnable_deletesMissingBook() {
        val missingBook = mockk<Book>(relaxed = true)
        every { missingBook.path } returns "/non/existent/book.pdf"
        every { anyConstructed<Storage>().listBook(any()) } returns listOf(missingBook)
        
        val scanner = ScannerBook(context)
        
        val runnableClass = ScannerBook::class.java.declaredClasses.find { it.name.contains("LibraryUpdateRunnable") }
        val constructor = runnableClass?.getDeclaredConstructor(ScannerBook::class.java, UUID::class.java, Library::class.java, Boolean::class.java)
        constructor?.isAccessible = true
        val runnableObj = constructor?.newInstance(scanner, UUID.randomUUID(), library, true)
        
        val runMethod = runnableClass?.getMethod("run")
        runMethod?.invoke(runnableObj)
        
        verify { anyConstructed<Storage>().delete(book = missingBook) }
    }
}
