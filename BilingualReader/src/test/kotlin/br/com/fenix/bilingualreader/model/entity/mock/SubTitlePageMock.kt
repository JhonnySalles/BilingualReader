package br.com.fenix.bilingualreader.model.entity.mock

import br.com.fenix.bilingualreader.model.entity.SubTitlePage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull

object SubTitlePageMock {

    fun mockEntity(): SubTitlePage = SubTitlePage(
        name = "page1.jpg",
        number = 1,
        hash = "hash123",
        subTitleTexts = listOf(SubTitleTextMock.mockEntity()),
        vocabulary = mutableSetOf()
    )

    fun mockEntityList(): List<SubTitlePage> = listOf(mockEntity())

    fun asserts(expected: SubTitlePage?, actual: SubTitlePage?) {
        assertNotNull("Actual SubTitlePage should not be null", actual)
        expected?.let {
            assertEquals("Name mismatch", it.name, actual?.name)
            assertEquals("Number mismatch", it.number, actual?.number)
            assertEquals("Hash mismatch", it.hash, actual?.hash)
        }
    }
}
