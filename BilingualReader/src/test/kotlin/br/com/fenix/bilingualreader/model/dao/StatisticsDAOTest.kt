package br.com.fenix.bilingualreader.model.dao

import br.com.fenix.bilingualreader.model.entity.mock.BookMock
import br.com.fenix.bilingualreader.model.entity.mock.HistoryMock
import br.com.fenix.bilingualreader.model.entity.mock.MangaMock
import br.com.fenix.bilingualreader.model.enums.Type
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
class StatisticsDAOTest : DataBaseBaseTest() {

    @Test
    fun getGlobalStatistics() {
        val mangaDao = db.getMangaDao()
        val bookDao = db.getBookDao()
        val historyDao = db.getHistoryDao()
        val statsDao = db.getStatisticsDao()

        val manga = MangaMock.mockEntity(1L).apply {
            pages = 100
            bookMark = 50
            excluded = false
        }
        mangaDao.save(manga)

        val book = BookMock.mockEntity(1L).apply {
            pages = 200
            bookMark = 0
            excluded = false
        }
        bookDao.save(book)

        // Add history for Manga
        val historyManga = HistoryMock.mockEntity(1L).copy(
            type = Type.MANGA,
            fkReference = 1L,
            pageStart = 0
        ).apply {
            setPageEnd(50)
            completed = false
        }
        // secondsRead is private and calculated from start/end in History.kt, 
        // but if we need a specific value we'd need to set end time.
        // For tests, setPageEnd usually handles completion.
        historyDao.save(historyManga)

        val stats = statsDao.statistics()
        
        // Should have 2 entries: MANGA and BOOK
        assertEquals(2, stats.size)
        
        val mangaStats = stats.find { it.type == Type.MANGA }
        assertNotNull(mangaStats)
        assertEquals(1L, mangaStats?.reading)
        assertEquals(1L, mangaStats?.library)
        assertEquals(50L, mangaStats?.totalReadPages)
        assertEquals(3600L, mangaStats?.totalReadSeconds)

        val bookStats = stats.find { it.type == Type.BOOK }
        assertNotNull(bookStats)
        assertEquals(1L, bookStats?.toRead)
        assertEquals(1L, bookStats?.library)
    }

    @Test
    fun getYearlyStatistics() {
        val bookDao = db.getBookDao()
        val historyDao = db.getHistoryDao()
        val statsDao = db.getStatisticsDao()

        val book = BookMock.mockEntity(1L).apply { pages = 100; bookMark = 100 }
        bookDao.save(book)

        val now = LocalDateTime.now()
        val history = HistoryMock.mockEntity(1L).copy(
            type = Type.BOOK,
            fkReference = 1L,
            pageStart = 0,
            start = now
        ).apply {
            setPageEnd(100)
            completed = true
        }
        historyDao.save(history)

        val stats = statsDao.statisticsBook(now.minusMonths(1), now.plusMonths(1))
        assertFalse(stats.isEmpty())
        assertEquals(1L, stats[0].read)
    }
}
