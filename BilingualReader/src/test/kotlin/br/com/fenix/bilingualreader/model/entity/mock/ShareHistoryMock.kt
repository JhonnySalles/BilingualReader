package br.com.fenix.bilingualreader.model.entity.mock

import br.com.fenix.bilingualreader.model.entity.ShareHistory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import java.util.*

object ShareHistoryMock {

    fun mockEntity(): ShareHistory = ShareHistory(
        pageStart = 1,
        pageEnd = 50,
        pages = 100,
        completed = false,
        volume = "1",
        chaptersRead = 5,
        start = Date(),
        end = Date(),
        secondsRead = 3600L,
        averageTimeByPage = 72L,
        useTTS = false
    )

    fun mockEntityList(): List<ShareHistory> = listOf(mockEntity())

    fun asserts(expected: ShareHistory?, actual: ShareHistory?) {
        assertNotNull("Actual shared history should not be null", actual)
        expected?.let {
            assertEquals("Page start mismatch", it.pageStart, actual?.pageStart)
            assertEquals("Page end mismatch", it.pageEnd, actual?.pageEnd)
            assertEquals("Volume mismatch", it.volume, actual?.volume)
        }
    }
}
