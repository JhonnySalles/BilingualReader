package br.com.fenix.bilingualreader.model.entity

import br.com.fenix.bilingualreader.model.enums.Type
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class StatisticsTest {

    @Test
    fun `test statistics construction`() {
        val now = LocalDateTime.now()
        val stats = Statistics(
            reading = 1L,
            toRead = 2L,
            library = 3L,
            read = 4L,
            completeReadingPages = 100L,
            completeReadingSeconds = 3600L,
            currentReadingPages = 50L,
            currentReadingSeconds = 1800L,
            totalReadPages = 150L,
            totalReadSeconds = 5400L,
            readByMonth = 10L,
            dateTime = now,
            type = Type.MANGA
        )

        assertEquals(1L, stats.reading)
        assertEquals(2L, stats.toRead)
        assertEquals(3L, stats.library)
        assertEquals(4L, stats.read)
        assertEquals(100L, stats.completeReadingPages)
        assertEquals(3600L, stats.completeReadingSeconds)
        assertEquals(50L, stats.currentReadingPages)
        assertEquals(1800L, stats.currentReadingSeconds)
        assertEquals(150L, stats.totalReadPages)
        assertEquals(5400L, stats.totalReadSeconds)
        assertEquals(10L, stats.readByMonth)
        assertEquals(now, stats.dateTime)
        assertEquals(Type.MANGA, stats.type)
    }
}
