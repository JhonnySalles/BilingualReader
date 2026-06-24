package br.com.fenix.bilingualreader.model.dao

import br.com.fenix.bilingualreader.model.entity.mock.LinkedFileMock
import br.com.fenix.bilingualreader.model.entity.mock.LinkedPageMock
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PageLinkDAOTest : DataBaseBaseTest() {

    @Test
    fun saveAndGetPageLink() {
        val dao = db.getPageLinkDao()
        val pageLink = LinkedPageMock.mockEntity(1L).apply {
            idFile = 100L
            isNotLinked = false
        }

        val id = dao.save(pageLink)
        assertEquals(1L, id)

        val list = dao.getPageLink(100L)
        assertEquals(1, list.size)
        assertEquals(1L, list[0].id)
    }

    @Test
    fun getPageNotLink() {
        val dao = db.getPageLinkDao()
        val page1 = LinkedPageMock.mockEntity(1L).apply { idFile = 100L; isNotLinked = false }
        val page2 = LinkedPageMock.mockEntity(2L).apply { idFile = 100L; isNotLinked = true }

        dao.save(page1)
        dao.save(page2)

        val listNotLinked = dao.getPageNotLink(100L)
        assertEquals(1, listNotLinked.size)
        assertEquals(2L, listNotLinked[0].id)
    }

    @Test
    fun deleteAllPagesByFile() {
        val dao = db.getPageLinkDao()
        dao.save(LinkedPageMock.mockEntity(1L).apply { idFile = 100L })
        dao.save(LinkedPageMock.mockEntity(2L).apply { idFile = 100L })

        dao.deleteAll(100L)

        val list = dao.getPageLink(100L)
        assertTrue(list.isEmpty())
    }

    @Test
    fun deleteAllPagesByManga() {
        val fileDao = db.getFileLinkDao()
        val pageDao = db.getPageLinkDao()
        
        val file = LinkedFileMock.mockEntity(10L).apply { idManga = 808L }
        fileDao.save(file)

        val page = LinkedPageMock.mockEntity(1L).apply { idFile = 10L }
        pageDao.save(page)

        pageDao.deleteAllByManga(808L)

        val list = pageDao.getPageLink(10L)
        assertTrue(list.isEmpty())
    }
}
