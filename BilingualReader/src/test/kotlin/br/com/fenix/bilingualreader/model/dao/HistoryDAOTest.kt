package br.com.fenix.bilingualreader.model.dao

import br.com.fenix.bilingualreader.model.entity.mock.HistoryMock
import br.com.fenix.bilingualreader.model.enums.Type
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HistoryDAOTest : DataBaseBaseTest() {

    @Test
    fun saveAndGetLastHistory() {
        val dao = db.getHistoryDao()
        val history1 = HistoryMock.mockEntity(1L).copy(
            type = Type.BOOK,
            fkLibrary = 1L,
            fkReference = 10L
        )
        val history2 = HistoryMock.mockEntity(2L).copy(
            type = Type.BOOK,
            fkLibrary = 1L,
            fkReference = 10L
        )

        dao.save(history1)
        dao.save(history2)

        val last = dao.last(Type.BOOK, 1L, 10L)
        assertNotNull(last)
        assertEquals(2L, last?.id)
    }

    @Test
    fun notifyHistory() {
        val dao = db.getHistoryDao()
        val history = HistoryMock.mockEntity(1L).apply { isNotify = false }
        dao.save(history)

        dao.notify(1L)

        val result = dao.find(history.type, history.fkLibrary, history.fkReference)
        assertTrue(result[0].isNotify)
    }

    @Test
    fun findHistory() {
        val dao = db.getHistoryDao()
        val history1 = HistoryMock.mockEntity(1L).copy(
            type = Type.MANGA,
            fkLibrary = 1L,
            fkReference = 10L
        )
        val history2 = HistoryMock.mockEntity(2L).copy(
            type = Type.MANGA,
            fkLibrary = 1L,
            fkReference = 10L
        )

        dao.save(history1)
        dao.save(history2)

        val result = dao.find(Type.MANGA, 1L, 10L)
        assertEquals(2, result.size)
    }
}
