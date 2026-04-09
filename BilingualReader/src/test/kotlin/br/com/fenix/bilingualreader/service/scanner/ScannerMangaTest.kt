package br.com.fenix.bilingualreader.service.scanner

import android.content.Context
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.service.repository.Storage
import br.com.fenix.bilingualreader.service.parses.manga.Parse
import br.com.fenix.bilingualreader.service.parses.manga.ParseFactory
import br.com.fenix.bilingualreader.util.helpers.Notifications
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
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
        context = androidx.test.core.app.ApplicationProvider.getApplicationContext<Context>()
        
        // Mock static Notification helpers
        mockkObject(Notifications.NotificationUtils)
        every { Notifications.getNotification(any(), any(), any()) } returns mockk(relaxed = true)
        every { Notifications.getID() } returns 1
        
        // Mock Storage constructor
        mockkConstructor(Storage::class)
        every { anyConstructed<Storage>().listMangas(any()) } returns emptyList()
        every { anyConstructed<Storage>().listDeleted(any()) } returns emptyList()
        every { anyConstructed<Storage>().findMangaByPath(any()) } returns null
        every { anyConstructed<Storage>().save(manga = any<Manga>(), lastAlteration = any()) } returns 1L
        
        // Mock Firebase
        mockkStatic(FirebaseApp::class)
        every { FirebaseApp.initializeApp(any()) } returns mockk()
        mockkStatic(FirebaseCrashlytics::class)
        every { FirebaseCrashlytics.getInstance() } returns mockk(relaxed = true)

        mockkObject(ParseFactory.Factory)
        val parse = mockk<Parse>(relaxed = true)
        every { ParseFactory.Factory.create(any<String>()) } returns parse
        every { ParseFactory.Factory.create(any<File>()) } returns parse
        every { parse.numPages() } returns 1

        library = Library(id = 3L, title = "Manga Library", path = tempFolder.root.canonicalPath)
        // Ensure the directory is not empty so 'walked' flag is set to true in ScannerManga
        tempFolder.newFile("placeholder.txt")
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
        
        // Access private inner class using reflection
        val runnableClass = ScannerManga::class.java.declaredClasses.find { it.name.contains("LibraryUpdateRunnable") }
        val constructor = runnableClass?.getDeclaredConstructor(ScannerManga::class.java, UUID::class.java, Library::class.java, Boolean::class.java)
        constructor?.isAccessible = true
        val runnableObj = constructor?.newInstance(scanner, UUID.randomUUID(), library, true)
        
        // Use reflection to call run() to avoid ClassCastException
        val runMethod = runnableClass?.getMethod("run")
        runMethod?.invoke(runnableObj)
        
        // Verify the code reached the scan loop
        verify(atLeast = 1) { anyConstructed<Storage>().listMangas(any()) }

        // Verify that storage.save was called for the new manga
        verify(atLeast = 1) { anyConstructed<Storage>().save(manga = any<Manga>(), lastAlteration = any()) }
        verify(atLeast = 1) { 
            anyConstructed<Storage>().save(manga = match<Manga> { 
                it.path.contains("manga_test.zip", ignoreCase = true) 
            }, lastAlteration = any()) 
        }
    }

    @Test
    fun libraryUpdateRunnable_deletesMissingManga() {
        val missingManga = mockk<Manga>(relaxed = true)
        every { missingManga.path } returns "/non/existent/manga.zip"
        every { anyConstructed<Storage>().listMangas(any()) } returns listOf(missingManga)
        
        val scanner = ScannerManga(context)
        
        val runnableClass = ScannerManga::class.java.declaredClasses.find { it.name.contains("LibraryUpdateRunnable") }
        val constructor = runnableClass?.getDeclaredConstructor(ScannerManga::class.java, UUID::class.java, Library::class.java, Boolean::class.java)
        constructor?.isAccessible = true
        val runnableObj = constructor?.newInstance(scanner, UUID.randomUUID(), library, true)
        
        val runMethod = runnableClass?.getMethod("run")
        runMethod?.invoke(runnableObj)
        
        verify { anyConstructed<Storage>().delete(manga = missingManga) }
    }
}
