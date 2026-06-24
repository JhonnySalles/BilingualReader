package br.com.fenix.bilingualreader.model.entity.mock

import br.com.fenix.bilingualreader.model.entity.Information
import br.com.fenix.bilingualreader.model.enums.Languages
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull

object InformationMock {

    fun mockEntity(): Information = Information().apply {
        title = "Mock Information Title"
        link = "https://example.com"
        synopsis = "This is a mock synopsis for testing."
        volumes = "1"
        chapters = "10"
        status = "Finished"
        genres = "Action, Adventure"
        authors = "Mock Author"
        language = Languages.ENGLISH
        origin = Information.COMIC_INFO
    }

    fun mockEntityList(): List<Information> = listOf(
        mockEntity()
    )

    fun asserts(expected: Information?, actual: Information?) {
        assertNotNull("Actual information should not be null", actual)
        expected?.let {
            assertEquals("Title mismatch", it.title, actual?.title)
            assertEquals("Link mismatch", it.link, actual?.link)
            assertEquals("Language mismatch", it.language, actual?.language)
        }
    }
}
