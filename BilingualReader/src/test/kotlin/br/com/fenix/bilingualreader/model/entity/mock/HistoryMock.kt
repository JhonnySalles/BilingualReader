package br.com.fenix.bilingualreader.model.entity.mock

import br.com.fenix.bilingualreader.model.entity.History
import br.com.fenix.bilingualreader.model.enums.Type
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import java.time.LocalDateTime

object HistoryMock : Mock<Long, History> {

    override fun mockEntity(): History = mockEntity(1L)

    override fun mockEntityList(): List<History> = listOf(
        mockEntity(1L),
        mockEntity(2L)
    )

    override fun mockEntity(id: Long): History = History(
        id = id,
        fkLibrary = 1L,
        fkReference = 100L,
        type = Type.MANGA,
        pageStart = 1,
        pageEnd = 50,
        pages = 100,
        completed = false,
        volume = "1",
        chaptersRead = 5,
        start = LocalDateTime.now().minusHours(1),
        end = LocalDateTime.now(),
        secondsRead = 3600,
        averageTimeByPage = 72,
        useTTS = false,
        isNotify = false
    )

    override fun asserts(expected: History?, actual: History?) {
        assertNotNull("Actual history should not be null", actual)
        expected?.let {
            assertEquals("ID mismatch", it.id, actual?.id)
            assertEquals("FK Library mismatch", it.fkLibrary, actual?.fkLibrary)
            assertEquals("FK Reference mismatch", it.fkReference, actual?.fkReference)
            assertEquals("Type mismatch", it.type, actual?.type)
            assertEquals("Page end mismatch", it.getPageEnd(), actual?.getPageEnd())
            assertEquals("Completed status mismatch", it.completed, actual?.completed)
        }
    }
}
