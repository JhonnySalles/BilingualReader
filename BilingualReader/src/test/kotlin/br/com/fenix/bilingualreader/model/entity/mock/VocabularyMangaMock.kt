package br.com.fenix.bilingualreader.model.entity.mock

import br.com.fenix.bilingualreader.model.entity.VocabularyManga
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull

object VocabularyMangaMock : Mock<Long, VocabularyManga> {

    override fun mockEntity(): VocabularyManga = mockEntity(1L)

    override fun mockEntityList(): List<VocabularyManga> = listOf(
        mockEntity(1L),
        mockEntity(2L)
    )

    override fun mockEntity(id: Long): VocabularyManga = VocabularyManga(
        id = id,
        idVocabulary = 200L,
        idManga = 2000L,
        appears = 1
    )

    override fun asserts(expected: VocabularyManga?, actual: VocabularyManga?) {
        assertNotNull("Actual vocabulary manga should not be null", actual)
        expected?.let {
            assertEquals("ID mismatch", it.id, actual?.id)
            assertEquals("ID Vocabulary mismatch", it.idVocabulary, actual?.idVocabulary)
            assertEquals("ID Manga mismatch", it.idManga, actual?.idManga)
        }
    }
}
