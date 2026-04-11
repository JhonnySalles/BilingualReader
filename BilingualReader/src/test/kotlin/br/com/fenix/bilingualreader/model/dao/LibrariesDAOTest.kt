package br.com.fenix.bilingualreader.model.dao

import br.com.fenix.bilingualreader.model.entity.mock.LibraryMock
import br.com.fenix.bilingualreader.model.enums.Libraries
import br.com.fenix.bilingualreader.model.enums.Type
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LibrariesDAOTest : DataBaseBaseTest() {

    @Test
    fun saveAndGetLibrary() {
        val dao = db.getLibrariesDao()
        val library = LibraryMock.mockEntity(1L)

        val id = dao.save(library)
        assertEquals(1L, id)

        val result = dao.get(1L)
        assertNotNull(result)
        LibraryMock.asserts(library, result)
    }

    @Test
    fun listEnabledLibraries() {
        val dao = db.getLibrariesDao()
        val lib1 = LibraryMock.mockEntity(1L).apply { enabled = true }
        val lib2 = LibraryMock.mockEntity(2L).apply { enabled = false }

        dao.save(lib1)
        dao.save(lib2)

        val enabledList = dao.listEnabled()
        assertEquals(1, enabledList.size)
        assertEquals(1L, enabledList[0].id)
    }

    @Test
    fun getByTypeAndLanguage() {
        val dao = db.getLibrariesDao()
        val library = LibraryMock.mockEntity(1L).apply {
            type = Type.MANGA
            language = Libraries.JAPANESE
        }

        dao.save(library)

        val result = dao.get(Type.MANGA, Libraries.JAPANESE)
        assertNotNull(result)
        assertEquals(1L, result?.id)
    }

    @Test
    fun deleteLibrary() {
        val dao = db.getLibrariesDao()
        val library = LibraryMock.mockEntity(1L)
        dao.save(library)

        dao.delete(1L)

        val result = dao.get(1L)
        assertEquals(true, result.excluded)

        val list = dao.list()
        assertTrue(list.isEmpty())
    }
}
