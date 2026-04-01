package br.com.fenix.bilingualreader.model.dao

import br.com.fenix.bilingualreader.model.entity.mock.MangaMock
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MangaDAOTest : DataBaseBaseTest() {

    @Test
    fun saveAndGetManga() {
        val dao = db.getMangaDao()
        val manga = MangaMock.mockEntity(1L)

        val id = dao.save(manga)
        assertEquals(1L, id)

        val result = dao.get(1L)
        assertNotNull(result)
        MangaMock.asserts(manga, result)
    }

    @Test
    fun listByLibrary() {
        val dao = db.getMangaDao()
        val manga1 = MangaMock.mockEntity(1L).apply { fkLibrary = 10L }
        val manga2 = MangaMock.mockEntity(2L).apply { fkLibrary = 10L }
        val manga3 = MangaMock.mockEntity(3L).apply { fkLibrary = 20L }

        dao.save(manga1)
        dao.save(manga2)
        dao.save(manga3)

        val list10 = dao.list(10L)
        assertEquals(2, list10.size)
        // Verify both returned manga have idLibrary = 10L
        assertTrue(list10.all { it.fkLibrary == 10L })
    }

    @Test
    fun deleteManga() {
        val dao = db.getMangaDao()
        val manga = MangaMock.mockEntity(1L)
        dao.save(manga)

        dao.delete(1L)

        val result = dao.get(1L)
        assertNull(result)
    }

    @Test
    fun updateBookMark() {
        val dao = db.getMangaDao()
        val manga = MangaMock.mockEntity(1L).apply { bookMark = 0 }
        dao.save(manga)

        dao.updateBookMark(1L, 50)

        val result = dao.get(1L)
        assertEquals(50, result?.bookMark)
    }

    @Test
    fun getByFileName() {
        val dao = db.getMangaDao()
        val manga = MangaMock.mockEntity(1L).apply { name = "MangaFile.cbz" }
        dao.save(manga)

        val result = dao.getByFileName("MangaFile.cbz")
        assertNotNull(result)
        assertEquals(1L, result?.id)
    }

    @Test
    fun getByPath() {
        val dao = db.getMangaDao()
        val manga = MangaMock.mockEntity(1L).apply { path = "/manga/file.cbz" }
        dao.save(manga)

        val result = dao.getByPath("/manga/file.cbz")
        assertNotNull(result)
        assertEquals(1L, result?.id)
    }
}
