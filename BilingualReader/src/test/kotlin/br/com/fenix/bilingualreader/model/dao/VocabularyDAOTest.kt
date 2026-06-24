package br.com.fenix.bilingualreader.model.dao

import br.com.fenix.bilingualreader.model.entity.mock.VocabularyMock
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class VocabularyDAOTest : DataBaseBaseTest() {

    @Test
    fun saveAndGetVocabulary() {
        val dao = db.getVocabularyDao()
        val vocabulary = VocabularyMock.mockEntity(1L).copy(
            word = "食べる",
            basicForm = "食べる"
        )

        val id = dao.save(vocabulary)
        assertEquals(1L, id)

        val result = dao.get(1L)
        assertNotNull(result)
        VocabularyMock.asserts(vocabulary, result)
    }

    @Test
    fun findVocabulary() {
        val dao = db.getVocabularyDao()
        val vocabulary = VocabularyMock.mockEntity(1L).copy(word = "私")
        dao.save(vocabulary)

        val result = dao.find("私")
        assertNotNull(result)
        assertEquals(1L, result?.id)
    }

    @Test
    fun existsVocabulary() {
        val dao = db.getVocabularyDao()
        val vocabulary = VocabularyMock.mockEntity(1L).copy(word = "猫", basicForm = "猫")
        dao.save(vocabulary)

        val result = dao.exists("猫", "猫")
        assertNotNull(result)
    }

    @Test
    fun listWithPagination() {
        val dao = db.getVocabularyDao()
        for (i in 1..10) {
            dao.save(VocabularyMock.mockEntity(i.toLong()).copy(word = "Word$i"))
        }

        val list = dao.list(false, "word", false, 0, 5)
        assertEquals(5, list.size)
    }
}
