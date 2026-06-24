package br.com.fenix.bilingualreader.model.entity.mock

import br.com.fenix.bilingualreader.model.entity.ComicInfo
import br.com.fenix.bilingualreader.model.enums.ComicInfoManga
import br.com.fenix.bilingualreader.model.enums.ComicInfoYesNo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull

object ComicInfoMock {

    fun mockEntity(): ComicInfo = ComicInfo().apply {
        title = "Mock Comic Title"
        series = "Mock Series"
        number = 1.0f
        volume = 1
        writer = "Mock Writer"
        publisher = "Mock Publisher"
        genre = "Fantasy"
        languageISO = "en"
        manga = ComicInfoManga.Yes
        blackAndWhite = ComicInfoYesNo.Yes
        pages = ComicInfoPageMock.mockEntityList()
        pageCount = 1
    }

    fun mockEntityList(): List<ComicInfo> = listOf(mockEntity())

    fun asserts(expected: ComicInfo?, actual: ComicInfo?) {
        assertNotNull("Actual ComicInfo should not be null", actual)
        expected?.let {
            assertEquals("Title mismatch", it.title, actual?.title)
            assertEquals("Series mismatch", it.series, actual?.series)
            assertEquals("Volume mismatch", it.volume, actual?.volume)
            assertEquals("Page count mismatch", it.pageCount, actual?.pageCount)
        }
    }
}
