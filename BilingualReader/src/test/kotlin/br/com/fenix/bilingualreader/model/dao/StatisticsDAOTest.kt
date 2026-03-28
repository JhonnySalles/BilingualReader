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
        val historyManga = HistoryMock.mockEntity(1L).apply {
            type = Type.MANGA
            idReference = 1L
            pageStart = 0
            pageEnd = 50
            secondsRead = 3600
            completed = false
        }
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
        val history = HistoryMock.mockEntity(1L).apply {
            type = Type.BOOK
            idReference = 1L
            completed = true
            pageStart = 0
            pageEnd = 100
            dateTimeStart = now
        }
        historyDao.save(history)

        val stats = statsDao.statisticsBook(now.minusMonths(1), now.plusMonths(1))
        assertFalse(stats.isEmpty())
        assertEquals(1L, stats[0].read)
    }
}
