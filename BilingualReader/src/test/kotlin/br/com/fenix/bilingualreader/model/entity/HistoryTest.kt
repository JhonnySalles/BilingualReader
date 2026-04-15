package br.com.fenix.bilingualreader.model.entity

import br.com.fenix.bilingualreader.model.enums.Type
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class HistoryTest {

    @Test
    fun `test history construction and ignored constructor`() {
        val now = LocalDateTime.now()
        val history = History(
            fkLibrary = 1L,
            fkReference = 2L,
            type = Type.MANGA,
            pageStart = 0,
            pages = 10,
            volume = "1"
        )

        assertEquals(1L, history.fkLibrary)
        assertEquals(2L, history.fkReference)
        assertEquals(Type.MANGA, history.type)
        assertEquals(0, history.pageStart)
        assertEquals(10, history.getPages())
        assertFalse(history.completed)
        assertEquals(0L, history.getSecondsRead())
    }

    @Test
    fun `test setEnd updates secondsRead`() {
        val start = LocalDateTime.of(2023, 1, 1, 10, 0, 0)
        val history = History(
            id = null, fkLibrary = 1L, fkReference = 2L, type = Type.BOOK,
            pageStart = 0, pageEnd = 0, pages = 10, completed = false,
            volume = "1", chaptersRead = 0, start = start, end = start,
            secondsRead = 0, averageTimeByPage = 0, useTTS = false, isNotify = false
        )

        val end = start.plusSeconds(120) // 2 minutes
        history.setEnd(end)

        assertEquals(end, history.getEnd())
        assertEquals(120L, history.getSecondsRead())
    }

    @Test
    fun `test setPageEnd updates completed status`() {
        val history = History(1L, 2L, Type.MANGA, 0, 10, "1")
        
        history.setPageEnd(5)
        assertEquals(5, history.getPageEnd())
        assertFalse(history.completed)

        history.setPageEnd(10)
        assertTrue("Completed should be true when pageEnd >= pages", history.completed)

        history.setPageEnd(15)
        assertTrue(history.completed)
    }

    @Test
    fun `test setPages updates completed status`() {
        val history = History(1L, 2L, Type.MANGA, 0, 10, "1")
        history.setPageEnd(8)
        assertFalse(history.completed)

        history.setPages(5)
        assertTrue("Completed should be true if new pages count is less than current pageEnd", history.completed)
    }
}
