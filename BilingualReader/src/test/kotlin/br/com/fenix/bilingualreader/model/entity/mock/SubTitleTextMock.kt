package br.com.fenix.bilingualreader.model.entity.mock

import br.com.fenix.bilingualreader.model.entity.SubTitleText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull

object SubTitleTextMock {

    fun mockEntity(): SubTitleText = SubTitleText(
        text = "Mock subtitle text",
        sequence = 1,
        x1 = 10,
        y1 = 10,
        x2 = 100,
        y2 = 20
    )

    fun mockEntityList(): List<SubTitleText> = listOf(mockEntity())

    fun asserts(expected: SubTitleText?, actual: SubTitleText?) {
        assertNotNull("Actual SubTitleText should not be null", actual)
        expected?.let {
            assertEquals("Text mismatch", it.text, actual?.text)
            assertEquals("Sequence mismatch", it.sequence, actual?.sequence)
        }
    }
}
