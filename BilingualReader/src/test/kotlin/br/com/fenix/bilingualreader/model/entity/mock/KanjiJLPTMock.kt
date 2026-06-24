package br.com.fenix.bilingualreader.model.entity.mock

import br.com.fenix.bilingualreader.model.entity.KanjiJLPT
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull

object KanjiJLPTMock : Mock<Long, KanjiJLPT> {

    override fun mockEntity(): KanjiJLPT = mockEntity(1L)

    override fun mockEntityList(): List<KanjiJLPT> = listOf(
        mockEntity(1L),
        mockEntity(2L)
    )

    override fun mockEntity(id: Long): KanjiJLPT = KanjiJLPT(
        id = id,
        kanji = "Mock Kanji $id",
        level = 5
    )

    override fun asserts(expected: KanjiJLPT?, actual: KanjiJLPT?) {
        assertNotNull("Actual KanjiJLPT should not be null", actual)
        expected?.let {
            assertEquals("ID mismatch", it.id, actual?.id)
            assertEquals("Kanji mismatch", it.kanji, actual?.kanji)
            assertEquals("Level mismatch", it.level, actual?.level)
        }
    }
}
