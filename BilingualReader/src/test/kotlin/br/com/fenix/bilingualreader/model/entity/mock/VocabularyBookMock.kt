package br.com.fenix.bilingualreader.model.entity.mock

import br.com.fenix.bilingualreader.model.entity.VocabularyBook
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull

object VocabularyBookMock : Mock<Long, VocabularyBook> {

    override fun mockEntity(): VocabularyBook = mockEntity(1L)

    override fun mockEntityList(): List<VocabularyBook> = listOf(
        mockEntity(1L),
        mockEntity(2L)
    )

    override fun mockEntity(id: Long): VocabularyBook = VocabularyBook(
        id = id,
        idVocabulary = 100L,
        idBook = 1000L,
        appears = 1
    )

    override fun asserts(expected: VocabularyBook?, actual: VocabularyBook?) {
        assertNotNull("Actual vocabulary book should not be null", actual)
        expected?.let {
            assertEquals("ID mismatch", it.id, actual?.id)
            assertEquals("ID Vocabulary mismatch", it.idVocabulary, actual?.idVocabulary)
            assertEquals("ID Book mismatch", it.idBook, actual?.idBook)
        }
    }
}
