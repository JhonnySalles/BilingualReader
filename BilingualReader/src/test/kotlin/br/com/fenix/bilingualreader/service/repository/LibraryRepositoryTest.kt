package br.com.fenix.bilingualreader.service.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.enums.Libraries
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.repository.LibrariesDAO
import br.com.fenix.bilingualreader.util.helpers.LibraryUtil
import io.mockk.*
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class LibraryRepositoryTest {

    private lateinit var context: Context
    private val librariesDao: LibrariesDAO = mockk(relaxed = true)
    private val dataBaseMock: DataBase = mockk(relaxed = true)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        mockkObject(DataBase)
        mockkObject(LibraryUtil)
        every { DataBase.getDataBase(any()) } returns dataBaseMock
        every { dataBaseMock.getLibrariesDao() } returns librariesDao
    }

    @Test
    fun testSave() {
        val repository = LibraryRepository(context)
        val library = Library(null, "Title", "/path", Libraries.JAPANESE, Type.MANGA)
        
        every { librariesDao.save(any<Library>()) } returns 123L
        
        val id = repository.save(library)
        
        assertEquals(123L, id)
        verify { librariesDao.save(any<Library>()) }
    }

    @Test
    fun testUpdate() {
        val repository = LibraryRepository(context)
        val library = Library(123L, "Title", "/path", Libraries.JAPANESE, Type.MANGA)
        
        every { librariesDao.update(any<Library>()) } returns 1
        
        repository.update(library)
        
        verify { librariesDao.update(any<Library>()) }
    }

    @Test
    fun testDelete() {
        val repository = LibraryRepository(context)
        val library = Library(123L, "Title", "/path", Libraries.JAPANESE, Type.MANGA)
        
        repository.delete(library)
        
        verify { librariesDao.delete(123L) }
    }
}
