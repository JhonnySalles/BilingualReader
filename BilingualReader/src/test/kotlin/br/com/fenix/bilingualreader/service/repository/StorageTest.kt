package br.com.fenix.bilingualreader.service.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.enums.Type
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class StorageTest {

    private lateinit var db: DataBase
    private lateinit var storage: Storage
    private lateinit var libraryRepository: LibraryRepository
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        
        // Mock Firebase to avoid IllegalStateException
        mockkStatic(Firebase::class)
        mockkStatic(FirebaseCrashlytics::class)
        mockkStatic("com.google.firebase.crashlytics.ktx.FirebaseCrashlyticsKt")
        
        val crashlytics = mockk<FirebaseCrashlytics>(relaxed = true)
        every { Firebase.crashlytics } returns crashlytics
        
        db = Room.inMemoryDatabaseBuilder(context, DataBase::class.java)
            .allowMainThreadQueries()
            .build()
            
        DataBase.setTestingInstance(db)

        storage = Storage(context)
        libraryRepository = LibraryRepository(context)
    }

    @After
    fun tearDown() {
        db.close()
        unmockkAll()
    }

    @Test
    fun `save manga should return id`() {
        val libId = libraryRepository.save(Library(id = null, title = "Lib", type = Type.MANGA))
        val manga = Manga(fkLibrary = libId, id = null, file = File("folder/path.zip"))
        val id = storage.save(manga = manga)
        
        val retrieved = storage.getManga(id)
        assertEquals("path", retrieved?.title)
    }

    @Test
    fun `save book should return id`() {
        val libId = libraryRepository.save(Library(id = null, title = "Lib", type = Type.BOOK))
        val book = Book(fkLibrary = libId, id = null, file = File("folder/path.epub"))
        val id = storage.save(book = book)
        
        val retrieved = storage.getBook(id)
        assertEquals("path", retrieved?.title)
    }

    @Test
    fun `getPrevManga should return previous manga in library order`() {
        val library = Library(id = null, title = "Lib", type = Type.MANGA)
        val libId = libraryRepository.save(library)
        library.id = libId

        val m1 = Manga(fkLibrary = libId, id = null, file = File("folder/a.zip"))
        val m2 = Manga(fkLibrary = libId, id = null, file = File("folder/b.zip"))
        val m3 = Manga(fkLibrary = libId, id = null, file = File("folder/c.zip"))
        
        storage.save(manga = m1)
        storage.save(manga = m2)
        storage.save(manga = m3)

        val retrievedC = storage.findMangaByName("c.zip")!!

        val prev = storage.getPrevManga(library, retrievedC)
        assertEquals("b", prev?.title)
    }

    @Test
    fun `getNextBook should return next book in library order`() {
        val library = Library(id = null, title = "Lib", type = Type.BOOK)
        val libId = libraryRepository.save(library)
        library.id = libId

        val b1 = Book(fkLibrary = libId, id = null, file = File("folder/a.epub"))
        val b2 = Book(fkLibrary = libId, id = null, file = File("folder/b.epub"))
        
        storage.save(book = b1)
        storage.save(book = b2)

        val retrievedA = storage.findBookByName("a.epub")!!

        val next = storage.getNextBook(library, retrievedA)
        assertEquals("b", next?.title)
    }
}
