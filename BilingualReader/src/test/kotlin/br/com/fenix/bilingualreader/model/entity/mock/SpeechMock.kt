package br.com.fenix.bilingualreader.model.entity.mock

import br.com.fenix.bilingualreader.model.entity.Speech
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull

object SpeechMock {

    fun mockEntity(): Speech = Speech(
        page = 1,
        sequence = 0,
        text = "Mock speech text content.",
        html = "<b>Mock speech text content.</b>",
        audio = null,
        media = null,
        isRead = false
    )

    fun mockEntityList(): List<Speech> = listOf(
        mockEntity()
    )

    fun asserts(expected: Speech?, actual: Speech?) {
        assertNotNull("Actual speech should not be null", actual)
        expected?.let {
            assertEquals("Page mismatch", it.page, actual?.page)
            assertEquals("Sequence mismatch", it.sequence, actual?.sequence)
            assertEquals("Text mismatch", it.text, actual?.text)
        }
    }
}
