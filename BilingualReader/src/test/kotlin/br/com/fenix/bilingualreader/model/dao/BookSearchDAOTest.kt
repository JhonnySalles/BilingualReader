package br.com.fenix.bilingualreader.model.dao

import br.com.fenix.bilingualreader.model.entity.mock.BookSearchMock
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
class BookSearchDAOTest : DataBaseBaseTest() {

    @Test
    fun saveAndListSearchHistory() {
        val dao = db.getBookSearch()
        val search1 = BookSearchMock.mockEntity(1L).copy(id_book = 202L).apply {
            search = "Magical Beasts"
            date = LocalDateTime.now().minusHours(1)
        }
        val search2 = BookSearchMock.mockEntity(2L).copy(id_book = 202L).apply {
            search = "Dragon Tales"
            date = LocalDateTime.now()
        }

        dao.save(search1)
        dao.save(search2)

        val list = dao.findAllByBook(202L)
        assertEquals(2, list.size)
        assertEquals("Dragon Tales", list[0].search)
        assertEquals("Magical Beasts", list[1].search)
    }
}
