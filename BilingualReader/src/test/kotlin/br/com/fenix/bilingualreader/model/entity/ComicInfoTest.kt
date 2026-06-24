package br.com.fenix.bilingualreader.model.entity

import br.com.fenix.bilingualreader.model.enums.ComicInfoAgeRating
import br.com.fenix.bilingualreader.model.enums.ComicInfoManga
import br.com.fenix.bilingualreader.model.enums.ComicInfoYesNo
import org.junit.Assert.assertEquals
import org.junit.Test

class ComicInfoTest {

    @Test
    fun `test comic info construction and defaults`() {
        val pages = listOf(ComicInfoPage(bookmark = "Start"))
        val info = ComicInfo(
            title = "Test Manga",
            series = "Test Series",
            number = 1.0f,
            volume = 1,
            writer = "Author",
            publisher = "Publisher",
            pageCount = 100,
            pages = pages,
            ageRating = ComicInfoAgeRating.Teen,
            manga = ComicInfoManga.Yes,
            blackAndWhite = ComicInfoYesNo.Yes
        )

        assertEquals("Test Manga", info.title)
        assertEquals("Test Series", info.series)
        assertEquals(1.0f, info.number)
        assertEquals(1, info.volume)
        assertEquals("Author", info.writer)
        assertEquals(100, info.pageCount)
        assertEquals(pages, info.pages)
        assertEquals(ComicInfoAgeRating.Teen, info.ageRating)
        assertEquals(ComicInfoManga.Yes, info.manga)
        assertEquals(ComicInfoYesNo.Yes, info.blackAndWhite)
    }

    @Test
    fun `test comic info empty constructor`() {
        val info = ComicInfo()
        assertEquals(null, info.title)
        assertEquals(null, info.pageCount)
        assertEquals(null, info.pages)
    }
}
