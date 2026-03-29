package br.com.fenix.bilingualreader.model.dao

import br.com.fenix.bilingualreader.model.entity.mock.LinkedFileMock
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
class FileLinkDAOTest : DataBaseBaseTest() {

    @Test
    fun saveAndGetFileLink() {
        val dao = db.getFileLinkDao()
        val fileLink = LinkedFileMock.mockEntity(1L).apply {
            idManga = 808L
            name = "Chapter 1"
            pages = 25
        }

        val id = dao.save(fileLink)
        assertEquals(1L, id)

        val resultByManga = dao.get(808L)
        assertEquals(1, resultByManga?.size)
        assertEquals("Chapter 1", resultByManga?.get(0)?.name)

        val resultSpecific = dao.get(808L, "Chapter 1", 25)
        assertNotNull(resultSpecific)
        assertEquals(1L, resultSpecific?.id)
    }

    @Test
    fun getLastAccess() {
        val dao = db.getFileLinkDao()
        val file1 = LinkedFileMock.mockEntity(1L).apply { idManga = 808L; lastAccess = LocalDateTime.now().minusDays(1) }
        val file2 = LinkedFileMock.mockEntity(2L).apply { idManga = 808L; lastAccess = LocalDateTime.now() }

        dao.save(file1)
        dao.save(file2)

        val last = dao.getLastAccess(808L)
        assertEquals(2L, last?.id)
    }

    @Test
    fun deleteFileLink() {
        val dao = db.getFileLinkDao()
        dao.save(LinkedFileMock.mockEntity(1L).apply { idManga = 808L; name = "Chapter 1" })

        dao.delete(808L, "Chapter 1")

        val list = dao.get(808L)
        assertTrue(list?.isEmpty() == true)
    }

    @Test
    fun deleteAllFileLinks() {
        val dao = db.getFileLinkDao()
        dao.save(LinkedFileMock.mockEntity(1L).apply { idManga = 808L })
        dao.save(LinkedFileMock.mockEntity(2L).apply { idManga = 808L })

        dao.deleteAllByManga(808L)

        val list = dao.get(808L)
        assertTrue(list?.isEmpty() == true)
    }
}
