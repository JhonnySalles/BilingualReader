package br.com.fenix.bilingualreader.model.entity

import br.com.fenix.bilingualreader.model.enums.ComicInfoPageType
import org.junit.Assert.assertEquals
import org.junit.Test

class ComicInfoPageTest {

    @Test
    fun `test comic info page construction`() {
        val page = ComicInfoPage(
            bookmark = "Ch 1",
            image = 1,
            imageHeight = 1000,
            imageWidth = 700,
            imageSize = 500000L,
            type = ComicInfoPageType.Story,
            doublePage = false,
            key = "key123"
        )

        assertEquals("Ch 1", page.bookmark)
        assertEquals(1, page.image)
        assertEquals(1000, page.imageHeight)
        assertEquals(700, page.imageWidth)
        assertEquals(500000L, page.imageSize)
        assertEquals(ComicInfoPageType.Story, page.type)
        assertEquals(false, page.doublePage)
        assertEquals("key123", page.key)
    }

    @Test
    fun `test comic info page default values`() {
        val page = ComicInfoPage()
        assertEquals(null, page.bookmark)
        assertEquals(null, page.image)
        assertEquals(null, page.doublePage)
    }
}
