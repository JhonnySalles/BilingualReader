package br.com.fenix.bilingualreader.service.scanner

import android.content.Context
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.entity.Manga
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
class ScannerMangaTest {

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
        
        // Mock Storage constructor to avoid real DB access
        mockkConstructor(Storage::class)
        every { anyConstructed<Storage>().listMangas(any()) } returns emptyList()
        every { anyConstructed<Storage>().listDeleted(any()) } returns emptyList()
        every { anyConstructed<Storage>().findMangaByPath(any()) } returns null
        every { anyConstructed<Storage>().save(manga = any<Manga>(), lastAlteration = any()) } returns 1L

        library = Library(id = 1L, title = "Test Library", path = tempFolder.root.absolutePath)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun libraryUpdateRunnable_detectsAndSavesNewManga() {
        // Create a fake manga file
        val mangaFile = tempFolder.newFile("manga_test.zip")
        
        val scanner = ScannerManga(context)
        
        // Access private inner class using reflection to run it synchronously
        val runnableClass = ScannerManga::class.java.declaredClasses.find { it.name.contains("LibraryUpdateRunnable") }
        val constructor = runnableClass?.getDeclaredConstructor(ScannerManga::class.java, UUID::class.java, Library::class.java, Boolean::class.java)
        constructor?.isAccessible = true
        
        // LibraryUpdateRunnable(id, library, isSilent)
        val runnable = constructor?.newInstance(scanner, UUID.randomUUID(), library, true) as Runnable
        
        // Execute scanning logic
        runnable.run()
        
        // Verify that storage.save was called at least once for the new manga
        verify(atLeast = 1) { 
            anyConstructed<Storage>().save(manga = match { it.path == mangaFile.absolutePath }, lastAlteration = any()) 
        }
    }

    @Test
    fun libraryUpdateRunnable_deletesMissingManga() {
        // Setup existing manga in storage that doesn't exist on disk
        val missingManga = mockk<Manga>(relaxed = true)
        every { missingManga.path } returns "/non/existent/path.zip"
        every { anyConstructed<Storage>().listMangas(any()) } returns listOf(missingManga)
        
        val scanner = ScannerManga(context)
        
        val runnableClass = ScannerManga::class.java.declaredClasses.find { it.name.contains("LibraryUpdateRunnable") }
        val constructor = runnableClass?.getDeclaredConstructor(ScannerManga::class.java, UUID::class.java, Library::class.java, Boolean::class.java)
        constructor?.isAccessible = true
        val runnable = constructor?.newInstance(scanner, UUID.randomUUID(), library, true) as Runnable
        
        runnable.run()
        
        // Verify that storage.delete was called for the missing manga
        verify { anyConstructed<Storage>().delete(missingManga) }
    }
}
