package br.com.fenix.bilingualreader.model.entity.mock

import br.com.fenix.bilingualreader.model.entity.Vocabulary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull

object VocabularyMock : Mock<Long, Vocabulary> {

    override fun mockEntity(): Vocabulary = mockEntity(1L)

    override fun mockEntityList(): List<Vocabulary> = listOf(
        mockEntity(1L),
        mockEntity(2L)
    )

    override fun mockEntity(id: Long): Vocabulary = Vocabulary(
        id = id,
        word = "Mock Word $id",
        portuguese = "Palavra Mock $id",
        english = "Mock Word $id",
        reading = "Reading $id",
        basicForm = "Basic Form $id",
        jlpt = 5,
        revised = false,
        favorite = false,
        appears = 1
    )

    override fun asserts(expected: Vocabulary?, actual: Vocabulary?) {
        assertNotNull("Actual vocabulary should not be null", actual)
        expected?.let {
            assertEquals("ID mismatch", it.id, actual?.id)
            assertEquals("Word mismatch", it.word, actual?.word)
            assertEquals("English mismatch", it.english, actual?.english)
        }
    }
}
