package br.com.fenix.bilingualreader.model.entity.mock

import br.com.fenix.bilingualreader.model.entity.Kanjax
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull

object KanjaxMock : Mock<Long, Kanjax> {

    override fun mockEntity(): Kanjax = mockEntity(1L)

    override fun mockEntityList(): List<Kanjax> = listOf(
        mockEntity(1L),
        mockEntity(2L)
    )

    override fun mockEntity(id: Long): Kanjax = Kanjax(
        id = id,
        kanji = "Mock Kanji $id",
        keyword = "Keyword $id",
        meaning = "Meaning $id",
        koohii = "Story $id",
        koohii2 = "Story 2 $id",
        onYomi = "On Yomi $id",
        kunYomi = "Kun Yomi $id",
        onWords = "On Words $id",
        kunWords = "Kun Words $id",
        jlpt = 5,
        grade = 1,
        frequence = 1,
        strokes = 5,
        variants = "",
        radical = "",
        parts = "",
        utf8 = "UTF8",
        sjis = "SJIS",
        keywordPt = "Palavra Chave $id",
        meaningPt = "Significado $id"
    )

    override fun asserts(expected: Kanjax?, actual: Kanjax?) {
        assertNotNull("Actual kanjax should not be null", actual)
        expected?.let {
            assertEquals("ID mismatch", it.id, actual?.id)
            assertEquals("Kanji mismatch", it.kanji, actual?.kanji)
            assertEquals("Keyword mismatch", it.keyword, actual?.keyword)
            assertEquals("Meaning mismatch", it.meaning, actual?.meaning)
        }
    }
}
