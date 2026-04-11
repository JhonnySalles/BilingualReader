package br.com.fenix.bilingualreader.service.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.entity.Manga
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class StorageTest {

    private lateinit var storage: Storage
    private lateinit var db: DataBase
    private lateinit var library: Library

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, DataBase::class.java)
            .allowMainThreadQueries()
            .build()
        DataBase.setTestingInstance(db)

        storage = Storage(context)
        library = Library(id = 1, title = "Test Lib")
        db.getLibrariesDao().save(library)
    }

    @After
    fun tearDown() {
        db.close()
        unmockkAll()
    }

    @Test
    fun `save and get manga should work`() {
        val file = File("/mnt/test.zip")
        val manga = Manga(fkLibrary = 1L, id = null, file = file)
        val id = storage.save(manga)
        assertNotNull(id)

        val found = storage.getManga(id)
        assertNotNull(found)
        assertEquals(id, found?.id)
    }

    @Test
    fun `save and get book should work`() {
        val file = File("/mnt/test.epub")
        val book = Book(fkLibrary = 1L, id = null, file = file)
        val id = storage.save(book)
        assertNotNull(id)

        val found = storage.getBook(id)
        assertNotNull(found)
        assertEquals(id, found?.id)
    }
    
    @Test
    fun `delete manga should work`() {
        val file = File("/mnt/test.zip")
        val manga = Manga(fkLibrary = 1L, id = null, file = file)
        val id = storage.save(manga)
        val toDelete = storage.getManga(id)!!
        
        storage.delete(toDelete)
        val found = storage.getManga(id)
        assertNull(found)
    }
}
