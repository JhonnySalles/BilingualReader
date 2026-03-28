package br.com.fenix.bilingualreader.model.dao

import br.com.fenix.bilingualreader.model.entity.mock.BookMock
import br.com.fenix.bilingualreader.model.entity.mock.LibraryMock
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BookDAOTest : DataBaseBaseTest() {

    @Test
    fun saveAndGetBook() {
        val dao = db.getBookDao()
        val book = BookMock.mockEntity(1L)

        val id = dao.save(book)
        assertEquals(1L, id)

        val result = dao.get(1L)
        assertNotNull(result)
        BookMock.asserts(book, result)
    }

    @Test
    fun listByLibrary() {
        val dao = db.getBookDao()
        val book1 = BookMock.mockEntity(1L).apply { idLibrary = 10L }
        val book2 = BookMock.mockEntity(2L).apply { idLibrary = 10L }
        val book3 = BookMock.mockEntity(3L).apply { idLibrary = 20L }

        dao.save(book1)
        dao.save(book2)
        dao.save(book3)

        val list10 = dao.list(10L)
        assertEquals(2, list10.size)
        assertTrue(list10.all { it.idLibrary == 10L })
    }

    @Test
    fun deleteBook() {
        val dao = db.getBookDao()
        val book = BookMock.mockEntity(1L)
        dao.save(book)

        dao.delete(1L)

        val result = dao.get(1L)
        assertNull(result)
    }

    @Test
    fun updateBookMark() {
        val dao = db.getBookDao()
        val book = BookMock.mockEntity(1L).apply { bookMark = 0 }
        dao.save(book)

        dao.updateBookMark(1L, 50)

        val result = dao.get(1L)
        assertEquals(50, result?.bookMark)
    }

    @Test
    fun listByFolder() {
        val dao = db.getBookDao()
        val book1 = BookMock.mockEntity(1L).apply { fileFolder = "FolderA" }
        val book2 = BookMock.mockEntity(2L).apply { fileFolder = "FolderB" }

        dao.save(book1)
        dao.save(book2)

        val listA = dao.listByFolder("FolderA")
        assertEquals(1, listA.size)
        assertEquals("FolderA", listA[0].fileFolder)
    }
}
